package uk.ac.warwick.tabula.profiles.commands

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.BeanProperty
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.PersonalTutor
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.NoUser
import uk.ac.warwick.tabula.profiles.services.docconversion.RawStudentRelationship
import uk.ac.warwick.tabula.profiles.services.docconversion.RawStudentRelationshipExtractor
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.util.core.StringUtils.hasText
import scala.collection.mutable.Buffer

class UploadPersonalTutorsCommand extends Command[Buffer[Unit]] with Daoisms with Logging {

	var userLookup = Wire.auto[UserLookupService]
	var profileService = Wire.auto[ProfileService]
	var personalTutorExtractor = Wire.auto[RawStudentRelationshipExtractor]
	
	var extractWarning = Wire.property("${profiles.relationship.upload.warning}")

	@BeanProperty var file: UploadedFile = new UploadedFile
	@BeanProperty var rawStudentRelationships: JList[RawStudentRelationship] = LazyLists.simpleFactory()

	private def filenameOf(path: String) = new java.io.File(path).getName

	def postExtractValidation(errors: Errors, department: Department) = {
		val uniIdsSoFar: mutable.Set[String] = mutable.Set()

		if (rawStudentRelationships != null && !rawStudentRelationships.isEmpty()) {
			for (i <- 0 until rawStudentRelationships.length) {
				val rawStudentRelationship = rawStudentRelationships.get(i)
				val newTarget = uniIdsSoFar.add(rawStudentRelationship.targetUniversityId)
				errors.pushNestedPath("rawStudentRelationships[" + i + "]")
				rawStudentRelationship.isValid = validateRawStudentRelationship(rawStudentRelationship, errors, newTarget, department)
				errors.popNestedPath()
			}
		}
	}

	def validateRawStudentRelationship(rawStudentRelationship: RawStudentRelationship, errors: Errors, newTarget: Boolean, department: Department) = {
		var valid = true
		valid = valid && setTargetMember(rawStudentRelationship, department, newTarget, errors)
		valid = valid && setAgentMember(rawStudentRelationship, errors)

		valid
	}
	
	private def setTargetMember(rawStudentRelationship: RawStudentRelationship, department: Department, newTarget: Boolean, errors: Errors): Boolean = {
		var valid: Boolean = true
		val targetUniId = rawStudentRelationship.targetUniversityId

		if (hasText(rawStudentRelationship.targetUniversityId)) {
			if (!UniversityId.isValid(rawStudentRelationship.targetUniversityId)) {
				errors.rejectValue("targetUniversityId", "uniNumber.invalid")
				valid = false
			} else if (!newTarget) {
//						// Warn (not error) if relationship for this member is already uploaded
//						profileService.findRelationship(PersonalTutor, targetUniId) match {
//							case Some(rel) => rawStudentRelationship.warningMessage = extractWarning
//						}
				errors.rejectValue("targetUniversityId", "uniNumber.duplicate.relationship")
				valid = false
			} else {
				try {
					rawStudentRelationship.targetMember = getMember(targetUniId)
					if (!rawStudentRelationship.targetMember.affiliatedDepartments.contains(department)) {
						errors.rejectValue("targetUniversityId", "uniNumber.wrong.department", Array(department.getName), "")
						valid = false
					}
				}
				catch {
					case e: ItemNotFoundException => {
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

	private def setAgentMember(rawStudentRelationship: RawStudentRelationship, errors: Errors):Boolean = {
		var valid: Boolean = true
		val agentUniId = rawStudentRelationship.agentUniversityId
		if (hasText(rawStudentRelationship.agentUniversityId)) {
			if (!UniversityId.isValid(agentUniId)) {
					errors.rejectValue("agentUniversityId", "uniNumber.invalid")
					valid = false
			} else {
				try {
					rawStudentRelationship.agentMember = getMember(agentUniId)
				}
				catch {
					case e: ItemNotFoundException => {
						//errors.rejectValue("agentUniversityId", e.getMessage())
						errors.rejectValue("agentUniversityId", "uniNumber.userNotFound")
						valid = false
					}
				}
			}
		} else if (!hasText(rawStudentRelationship.agentName)) {
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

	//override def applyInternal(): List[StudentRelationship] = transactional() {
	override def applyInternal() = transactional() {
		def savePersonalTutor(rawStudentRelationship: RawStudentRelationship) = {
			var agent = ""
			if (hasText(rawStudentRelationship.agentUniversityId))
				agent = rawStudentRelationship.agentUniversityId
			else
				agent = rawStudentRelationship.agentName
			val targetUniversityId = rawStudentRelationship.targetUniversityId
			var targetSprCode = ""
			val targetMember = profileService.getMemberByUniversityId(targetUniversityId) match {
				case None => throw new ItemNotFoundException("Can't find student " + targetUniversityId)
				case Some(mem) => targetSprCode = mem.sprCode
			}
			
			profileService.saveRelationship(PersonalTutor, targetSprCode, agent)

			logger.debug("Saved personal tutor for " + targetUniversityId)
		}

		// persist valid personal tutors
		rawStudentRelationships filter (_.isValid) map { 
			(rawStudentRelationship) => savePersonalTutor(rawStudentRelationship) 
		}

		//val studentRelationshipList = rawStudentRelationships filter (_.isValid) map { (rawStudentRelationship) => savePersonalTutor(rawStudentRelationship) }
		//studentRelationshipList.toList
	}

	def onBind {
		transactional() {
			file.onBind
			if (!file.attached.isEmpty()) {
				processFiles(file.attached)
			}

			def processFiles(files: Seq[FileAttachment]) {
				for (file <- files.filter(_.hasData)) {
					rawStudentRelationships addAll personalTutorExtractor.readXSSFExcelFile(file.dataStream)
				}
			}
		}
	}

	def describe(d: Description) = d.property("personalTutorCount", rawStudentRelationships.size)

}