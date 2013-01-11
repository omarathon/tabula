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
import uk.ac.warwick.tabula.profiles.services.docconversion.MemberRelationshipExtractor
import uk.ac.warwick.tabula.profiles.services.docconversion.RawMemberRelationship
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.PersonalTutor
import uk.ac.warwick.tabula.data.model.MemberRelationship


class UploadPersonalTutorsCommand extends Command[List[MemberRelationship]] with Daoisms with Logging {

	var userLookup = Wire.auto[UserLookupService]
	var profileService = Wire.auto[ProfileService]
	var personalTutorExtractor = Wire.auto[MemberRelationshipExtractor]
	
	var extractWarning = Wire.property("${profiles.relationship.upload.warning}")

	@BeanProperty var file: UploadedFile = new UploadedFile
	@BeanProperty var rawMemberRelationships: JList[RawMemberRelationship] = LazyLists.simpleFactory()

	private def filenameOf(path: String) = new java.io.File(path).getName

	def postExtractValidation(errors: Errors) = {
		val uniIdsSoFar: mutable.Set[String] = mutable.Set()

		if (rawMemberRelationships != null && !rawMemberRelationships.isEmpty()) {
			for (i <- 0 until rawMemberRelationships.length) {
				val rawMemberRelationship = rawMemberRelationships.get(i)
				val newSubject = uniIdsSoFar.add(rawMemberRelationship.subjectUniversityId)
				errors.pushNestedPath("memberRelationships[" + i + "]")
				rawMemberRelationship.isValid = validateMemberRelationship(rawMemberRelationship, errors, newSubject)
				errors.popNestedPath()
			}
		}
	}

	def validateMemberRelationship(rawMemberRelationship: RawMemberRelationship, errors: Errors, newPerson: Boolean) = {

		var noErrors = true
		// validate subject
		if (hasText(rawMemberRelationship.subjectUniversityId)) {
			if (!UniversityId.isValid(rawMemberRelationship.subjectUniversityId)) {
				errors.rejectValue("subjectUniversityId", "uniNumber.invalid")
				noErrors = false
			} else if (!newPerson) {
				errors.rejectValue("subjectUniversityId", "uniNumber.duplicate.relationship")
				noErrors = false
			} else {
				userLookup.getUserByWarwickUniId(rawMemberRelationship.subjectUniversityId) match {
					case FoundUser(u) =>
					case NoUser(u) => {
						errors.rejectValue("subjectUniversityId", "uniNumber.userNotFound", Array(rawMemberRelationship.subjectUniversityId), "")
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
			errors.rejectValue("subjectUniversityId", "NotEmpty")
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
			val subject = rawMemberRelationship.subjectUniversityId

			val relationship = profileService.findRelationship(PersonalTutor, subject).getOrElse(new MemberRelationship(agent, PersonalTutor, subject))
			
			session.saveOrUpdate(relationship)
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