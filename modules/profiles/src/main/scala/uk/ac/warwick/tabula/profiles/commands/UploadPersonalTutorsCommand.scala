package uk.ac.warwick.tabula.profiles.commands

import scala.collection.JavaConversions._
import scala.collection.mutable
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
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.NoUser
import uk.ac.warwick.tabula.profiles.services.docconversion.RawStudentRelationship
import uk.ac.warwick.tabula.profiles.services.docconversion.RawStudentRelationshipExtractor
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.helpers.StringUtils._
import scala.collection.mutable.Buffer
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.StudentMember
import org.springframework.validation.BindingResult

class UploadPersonalTutorsCommand(val department: Department) extends Command[Seq[StudentRelationship]] with Daoisms with Logging with BindListener with SelfValidating {

	PermissionCheck(Permissions.Profiles.PersonalTutor.Upload, mandatory(department))

	val acceptedExtensions = Seq(".xlsx")

	val userLookup = Wire.auto[UserLookupService]
	var profileService = Wire.auto[ProfileService]
	var personalTutorExtractor = Wire.auto[RawStudentRelationshipExtractor]

	var extractWarning = Wire.property("${profiles.relationship.upload.warning}")

	var file: UploadedFile = new UploadedFile
	var rawStudentRelationships: JList[RawStudentRelationship] = LazyLists.simpleFactory()

	private def filenameOf(path: String) = new java.io.File(path).getName

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

		if (rawStudentRelationship.targetUniversityId.hasText) {
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
						case Some(targetMember: StudentMember) => {
							rawStudentRelationship.targetMember = targetMember
							if (targetMember.studyDetails.sprCode == null) {
								errors.rejectValue("targetUniversityId", "member.sprCode.notFound")
								valid = false
							}
							if (!targetMember.affiliatedDepartments.contains(department)) {
								errors.rejectValue("targetUniversityId", "uniNumber.wrong.department", Array(department.name), "")
								valid = false
							}
						}
						case Some(nonStudentMember) => {
							errors.rejectValue("targetUniversityId", "member.sprCode.notFound")
							valid = false
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
		if (agentUniId.hasText) {
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
		} else if (!rawStudentRelationship.agentName.hasText) {
			// just check for some free text
			// TODO could look for name-like qualities (> 3 chars etc)
			errors.rejectValue("agentName", "NotEmpty")
			valid = false
		}
		valid
	}

	private def getMember(uniId: String): Option[Member] = {
		if (!uniId.hasText)
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
			if (rawStudentRelationship.agentUniversityId.hasText)
				rawStudentRelationship.agentUniversityId
			else
				rawStudentRelationship.agentName

			val targetUniId = rawStudentRelationship.targetUniversityId

			val targetSprCode = profileService.getMemberByUniversityId(targetUniId) match {
				// should never be None as validation has already checked for this
				case None => throw new IllegalStateException("Couldn't find member for " + targetUniId)
				case Some(mem: StudentMember) => mem.studyDetails.sprCode
				case Some(otherMember) => throw new IllegalStateException("Couldn't find student for " + targetUniId + " (non-student found)")
			}
			
			val currentRelationships = profileService.findCurrentRelationships(PersonalTutor, targetSprCode)
			
			// Does this relationship already exist?
			currentRelationships.find(_.agent == agent) match {
				case Some(existing) => existing
				case _ => {
					// End all existing relationships
					currentRelationships.foreach { rel =>
						rel.endDate = DateTime.now
						profileService.saveOrUpdate(rel)
					}
					
					// Save the new one
					val rel = profileService.saveStudentRelationship(PersonalTutor, targetSprCode, agent)

					logger.debug("Saved personal tutor for " + targetUniId)
			
					rel
				}
			}
		}

		// persist valid personal tutors
		rawStudentRelationships filter (_.isValid) map {
			(rawStudentRelationship) => savePersonalTutor(rawStudentRelationship)
		}
	}

	def onBind(result:BindingResult) {
		val fileNames = file.fileNames map (_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !acceptedExtensions.exists(s.endsWith))

		if (invalidFiles.size > 0) {
			if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
			else result.rejectValue("", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
		}

		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
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
	}

	def describe(d: Description) = d.property("personalTutorCount", rawStudentRelationships.size)

}
