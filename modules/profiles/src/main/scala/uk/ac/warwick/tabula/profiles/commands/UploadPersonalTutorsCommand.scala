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
				val newTarget = uniIdsSoFar.add(rawMemberRelationship.targetUniversityId)
				errors.pushNestedPath("rawMemberRelationships[" + i + "]")
				rawMemberRelationship.isValid = validateRawMemberRelationship(rawMemberRelationship, errors, newTarget, department)
				errors.popNestedPath()
			}
		}
	}

	def validateRawMemberRelationship(rawMemberRelationship: RawMemberRelationship, errors: Errors, newTarget: Boolean, department: Department) = {
		var valid = true
		valid = valid && setTargetMember(rawMemberRelationship, department, newTarget, errors)
		valid = valid && setAgentMember(rawMemberRelationship, errors)

		valid
	}
	
	private def setTargetMember(rawMemberRelationship: RawMemberRelationship, department: Department, newTarget: Boolean, errors: Errors): Boolean = {
		var valid: Boolean = true
		val targetUniId = rawMemberRelationship.targetUniversityId

		if (hasText(rawMemberRelationship.targetUniversityId)) {
			if (!UniversityId.isValid(rawMemberRelationship.targetUniversityId)) {
				errors.rejectValue("targetUniversityId", "uniNumber.invalid")
				valid = false
			} else if (!newTarget) {
				errors.rejectValue("targetUniversityId", "uniNumber.duplicate.relationship")
				valid = false
			} else {
				try {
					rawMemberRelationship.targetMember = getMember(targetUniId)
					if (!rawMemberRelationship.targetMember.affiliatedDepartments.contains(department)) {
						errors.rejectValue("targetUniversityId", "uniNumber.wrong.department", Array(department.getName), "")
						valid = false
					}
				}
				catch {
					case e: ItemNotFoundException => {
						//errors.rejectValue("targetUniId", e.getMessage())
						errors.rejectValue("targetUniversityId", "uniNumber.userNotFound")
						valid = false
					}
				}
			}
		} else {
			errors.rejectValue("targetUniId", "NotEmpty")
			valid = false
		}
		valid
	}

	private def setAgentMember(rawMemberRelationship: RawMemberRelationship, errors: Errors):Boolean = {
		var valid: Boolean = true
		val agentUniId = rawMemberRelationship.agentUniversityId
		if (hasText(rawMemberRelationship.agentUniversityId)) {
			if (!UniversityId.isValid(agentUniId)) {
					errors.rejectValue("agentUniversityId", "uniNumber.invalid")
					valid = false
			} else {
				try {
					rawMemberRelationship.agentMember = getMember(agentUniId)
				}
				catch {
					case e: ItemNotFoundException => {
						//errors.rejectValue("agentUniversityId", e.getMessage())
						errors.rejectValue("agentUniversityId", "uniNumber.userNotFound")
						valid = false
					}
				}
			}
		} else if (!hasText(rawMemberRelationship.agentName)) {
			// just check for some free text
			// TODO could look for name-like qualities (> 3 chars etc)
			errors.rejectValue("agentName", "NotEmpty")
			valid = false
		}
		valid
	}	
	
	private def getMember(uniId: String): Member = {
		userLookup.getUserByWarwickUniId(uniId) match {
			case FoundUser(u) => {
				profileService.getMemberByUniversityId(uniId).getOrElse[Member] {
					throw new ItemNotFoundException("uniNumber.member.missing")
				}
			}
			case NoUser(u) => {
				throw new ItemNotFoundException("uniNumber.userNotFound")
			}
		}
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