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
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.commands.SelfValidating

class UploadPersonalTutorsCommand(val department: Department) extends Command[Seq[StudentRelationship]] with Daoisms with Logging with BindListener with SelfValidating {
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Upload, department)

	var userLookup = Wire.auto[UserLookupService]
	var profileService = Wire.auto[ProfileService]
	var personalTutorExtractor = Wire.auto[RawStudentRelationshipExtractor]
	
	var extractWarning = Wire.property("${profiles.relationship.upload.warning}")

	@BeanProperty var file: UploadedFile = new UploadedFile
	@BeanProperty var rawStudentRelationships: JList[RawStudentRelationship] = LazyLists.simpleFactory()

	private def filenameOf(path: String) = new java.io.File(path).getName

	//def postExtractValidation(errors: Errors, department: Department) = {
	def validate(errors: Errors) = {
		val uniIdsSoFar: mutable.Set[String] = mutable.Set()

		if (rawStudentRelationships != null && !rawStudentRelationships.isEmpty()) {
			for (i <- 0 until rawStudentRelationships.length) {
				val rawStudentRelationship = rawStudentRelationships.get(i)
				val newTarget = uniIdsSoFar.add(rawStudentRelationship.targetUniversityId match {
					case null => "[missing value]"
					case id => id 
				})
				errors.pushNestedPath("rawStudentRelationships[" + i + "]")
				rawStudentRelationship.isValid = validateRawStudentRelationship(rawStudentRelationship, errors, newTarget, department)
				errors.popNestedPath()
			}
		}
	}

	def validateRawStudentRelationship(rawStudentRelationship: RawStudentRelationship, errors: Errors, newTarget: Boolean, department: Department): Boolean = {
		setAndValidateStudentMember(rawStudentRelationship, department, newTarget, errors) && 
			setAndValidateAgentMember(rawStudentRelationship, errors)
	}

	private def setAndValidateStudentMember(rawStudentRelationship: RawStudentRelationship, department: Department, newTarget: Boolean, errors: Errors): Boolean = {
		var valid: Boolean = true
		val targetUniId = rawStudentRelationship.targetUniversityId

		if (hasText(rawStudentRelationship.targetUniversityId)) {
			if (!UniversityId.isValid(rawStudentRelationship.targetUniversityId)) {
				errors.rejectValue("targetUniversityId", "uniNumber.invalid")
				valid = false
			} else if (!newTarget) {
				// student appears more than once within the spreadsheet
				errors.rejectValue("targetUniversityId", "uniNumber.duplicate.relationship")
				valid = false
			} else {
				try {
					getMember(targetUniId) match {
						case None => {
							errors.rejectValue("targetUniversityId", "uniNumber.userNotFound")
							valid = false
						}
						case Some(targetMember) => {
							rawStudentRelationship.targetMember = targetMember
							if (targetMember.sprCode == null) {
								errors.rejectValue("targetUniversityId", "member.sprCode.notFound")
								valid = false
							}
							if (!targetMember.affiliatedDepartments.contains(department)) {
								errors.rejectValue("targetUniversityId", "uniNumber.wrong.department", Array(department.getName), "")
								valid = false
							}
						}
					}
				}
			}
		} else {
			errors.rejectValue("targetUniversityId", "NotEmpty.uniNumber")
			valid = false
		}
		valid
	}

	private def setAndValidateAgentMember(rawStudentRelationship: RawStudentRelationship, errors: Errors):Boolean = {
		var valid: Boolean = true
		val agentUniId = rawStudentRelationship.agentUniversityId
		if (hasText(agentUniId)) {
			if (!UniversityId.isValid(agentUniId)) {
					errors.rejectValue("agentUniversityId", "uniNumber.invalid")
					valid = false
			} else {
				getMember(agentUniId) match {
					case None => {
						errors.rejectValue("agentUniversityId", "uniNumber.userNotFound")
						valid = false
					}
					case Some(agentMember) => 
						rawStudentRelationship.agentMember = agentMember
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
	
	private def getMember(uniId: String): Option[Member] = {
		if (!hasText(uniId)) 
			None
		else {
			userLookup.getUserByWarwickUniId(uniId) match {
				case FoundUser(u) => {
					profileService.getMemberByUniversityId(uniId)
				}
				case NoUser(u) => {
					None
				}
			}
		}
	}

	override def applyInternal(): Seq[StudentRelationship] = transactional() {
		def savePersonalTutor(rawStudentRelationship: RawStudentRelationship): StudentRelationship = {
			val agent = 
			if (hasText(rawStudentRelationship.agentUniversityId))
				rawStudentRelationship.agentUniversityId
			else
				rawStudentRelationship.agentName
				
			val targetUniId = rawStudentRelationship.targetUniversityId
			
			var targetSprCode = profileService.getMemberByUniversityId(targetUniId) match {
				// should never be None as validation has already checked for this
				case None => throw new IllegalStateException("Couldn't find member for " + targetUniId) 
				case Some(mem) => mem.sprCode
			}
			
			val rel = profileService.saveStudentRelationship(PersonalTutor, targetSprCode, agent)

			logger.debug("Saved personal tutor for " + targetUniId)
			
			rel
		}

		// persist valid personal tutors
		rawStudentRelationships filter (_.isValid) map { 
			(rawStudentRelationship) => savePersonalTutor(rawStudentRelationship)
		}
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
