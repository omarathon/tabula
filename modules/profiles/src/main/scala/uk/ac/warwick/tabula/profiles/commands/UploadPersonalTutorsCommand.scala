package uk.ac.warwick.tabula.profiles.commands

import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.mutable
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.util.core.StringUtils.hasText
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.helpers.NoUser
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.spring.Wire
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.profiles.services.docconversion.RawMemberRelationshipExtractor
import uk.ac.warwick.tabula.profiles.services.docconversion.RawMemberRelationship
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.PersonalTutor
import uk.ac.warwick.tabula.data.model.MemberRelationship
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.Department


class UploadPersonalTutorsCommand extends Command[List[MemberRelationship]] with Daoisms with Logging {

	var userLookup = Wire.auto[UserLookupService]
	var profileService = Wire.auto[ProfileService]
	var personalTutorExtractor = Wire.auto[RawMemberRelationshipExtractor]
	
	var extractWarning = Wire.property("${profiles.relationship.upload.warning}")

	@BeanProperty var file: UploadedFile = new UploadedFile
	@BeanProperty var rawMemberRelationships: JList[RawMemberRelationship] = LazyLists.simpleFactory()

	private def filenameOf(path: String) = new java.io.File(path).getName

	def postExtractValidation(errors: Errors, department: Department) = {
		val uniIdsSoFar: mutable.Set[String] = mutable.Set()

		if (rawMemberRelationships != null && !rawMemberRelationships.isEmpty()) {
			for (i <- 0 until rawMemberRelationships.length) {
				val rawMemberRelationship = rawMemberRelationships.get(i)
				val newtarget = uniIdsSoFar.add(rawMemberRelationship.targetUniversityId)
				errors.pushNestedPath("rawMemberRelationships[" + i + "]")
				rawMemberRelationship.isValid = validateRawMemberRelationship(rawMemberRelationship, errors, newtarget, department)
				errors.popNestedPath()
			}
		}
	}

	def validateRawMemberRelationship(rawMemberRelationship: RawMemberRelationship, errors: Errors, newPerson: Boolean, department: Department) = {

		var noErrors = true
		val targetUniId = rawMemberRelationship.targetUniversityId
		
		// validate target
		if (hasText(rawMemberRelationship.targetUniversityId)) {
			if (!UniversityId.isValid(rawMemberRelationship.targetUniversityId)) {
				errors.rejectValue("targetUniversityId", "uniNumber.invalid")
				noErrors = false
			} else if (!newPerson) {
					errors.rejectValue("targetUniversityId", "uniNumber.duplicate.relationship")
					noErrors = false
				} else {
				userLookup.getUserByWarwickUniId(targetUniId) match {
					case FoundUser(u) => {
						val member = profileService.getMemberByUniversityId(targetUniId).getOrElse[Member] { 
							// going with an exception rather than an error here, since we know the uni ID is OK
							// so we'd expect our db to have the corresponding member
							throw new ItemNotFoundException("Failed to determine member for user")
						}
						if (!member.affiliatedDepartments.contains(department)) {
							errors.rejectValue("targetUniversityId", "uniNumber.wrong.department", Array(department.getName), "")
							noErrors = false
						}
					}
					case NoUser(u) => {
						errors.rejectValue("targetUniversityId", "uniNumber.userNotFound", Array(rawMemberRelationship.targetUniversityId), "")
						noErrors = false
					}
				}
				// Warn if relationship for this member is already uploaded
//				assignment.feedbacks.find { (feedback) => feedback.universityId == memberRelationship.universityId && (feedback.hasMark || feedback.hasGrade) } match {
//					case Some(feedback) => {
//						memberRelationship.warningMessage = markWarning
//					}
//					case None => {}
//				}
			}
		} else {
			errors.rejectValue("targetUniversityId", "NotEmpty")
		}
		
		// validate agent
		if (hasText(rawMemberRelationship.agentUniversityId)) {
			if (!UniversityId.isValid(rawMemberRelationship.agentUniversityId)) {
					errors.rejectValue("agentUniversityId", "uniNumber.invalid")
					noErrors = false
				} else {
				userLookup.getUserByWarwickUniId(rawMemberRelationship.agentUniversityId) match {
					case FoundUser(u) =>
					case NoUser(u) => {
						errors.rejectValue("agentUniversityId", "uniNumber.userNotFound", Array(rawMemberRelationship.agentUniversityId), "")
						noErrors = false
					}
				}
			}
		} else if (!hasText(rawMemberRelationship.agentName)) {
			// just check for some free text
			// TODO could look for name-like qualities (> 3 chars etc)
			errors.rejectValue("agentName", "NotEmpty")
			noErrors = false
		}
		
		noErrors
	}

	override def applyInternal(): List[MemberRelationship] = transactional() {
		def savePersonalTutor(rawMemberRelationship: RawMemberRelationship) = {
			var agent = ""
			if (hasText(rawMemberRelationship.agentUniversityId))
				agent = rawMemberRelationship.agentUniversityId
			else
				agent = rawMemberRelationship.agentName
			val target = rawMemberRelationship.targetUniversityId

			val oldRelationship = profileService.findRelationship(PersonalTutor, target)
			val relationship = oldRelationship match {
				case None => MemberRelationship(agent, PersonalTutor, target)
				case Some(rel) => {
					rel.setAgent(agent)
					rel
				}
			}
			
			session.saveOrUpdate(relationship)

			logger.debug("Saved personal tutor for " + target)
			
			relationship
		}

		// persist valid personal tutors
		val memberRelationshipList = rawMemberRelationships filter (_.isValid) map { (rawMemberRelationship) => savePersonalTutor(rawMemberRelationship) }
		memberRelationshipList.toList
	}

	def onBind {
		transactional() {
			file.onBind
			if (!file.attached.isEmpty()) {
				processFiles(file.attached)
			}

			def processFiles(files: Seq[FileAttachment]) {
				for (file <- files.filter(_.hasData)) {
					rawMemberRelationships addAll personalTutorExtractor.readXSSFExcelFile(file.dataStream)
				}
			}
		}
	}

	def describe(d: Description) = d.property("personalTutorCount", rawMemberRelationships.size)

}