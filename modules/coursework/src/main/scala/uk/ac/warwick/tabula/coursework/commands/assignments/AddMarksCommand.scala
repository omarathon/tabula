package uk.ac.warwick.tabula.coursework.commands.assignments

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.commands.{Command, Description, UploadedFile}
import uk.ac.warwick.tabula.coursework.services.docconversion.{MarkItem, MarksExtractor}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Module}
import uk.ac.warwick.tabula.helpers.{FoundUser, LazyLists, Logging, NoUser}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{GeneratesGradesFromMarks, UserLookupService}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._
import scala.collection.mutable


abstract class AddMarksCommand[A](val module: Module, val assignment: Assignment, val marker: User, val gradeGenerator: GeneratesGradesFromMarks) extends Command[A]
	with Daoisms with Logging with BindListener {

	val validAttachmentStrings = Seq(".xlsx")
	var userLookup = Wire.auto[UserLookupService]
	var marksExtractor = Wire.auto[MarksExtractor]
  
	var file: UploadedFile = new UploadedFile
	var marks: JList[MarkItem] = LazyLists.create()

	private def filenameOf(path: String) = new java.io.File(path).getName

	def postExtractValidation(errors: Errors) {
		val uniIdsSoFar: mutable.Set[String] = mutable.Set()

		if (marks != null && !marks.isEmpty) {
			for (i <- 0 until marks.length) {
				val mark = marks.get(i)
				val newPerson = if (mark.universityId != null){
					uniIdsSoFar.add(mark.universityId)
				} else {
					false
				}
				errors.pushNestedPath("marks[" + i + "]")
				mark.isValid = validateMarkItem(mark, errors, newPerson)
				errors.popNestedPath()
			}
		}
	}

	def checkMarkUpdated(mark: MarkItem)

	def validateMarkItem(mark: MarkItem, errors: Errors, newPerson: Boolean) = {

		var hasErrors = false
		// validate id
		if (mark.universityId.hasText) {
			if (!UniversityId.isValid(mark.universityId)) {
				errors.rejectValue("universityId", "uniNumber.invalid")
				hasErrors = true
			} else if (!newPerson) {
				errors.rejectValue("universityId", "uniNumber.duplicate.mark")
				hasErrors = true
			} else {
				userLookup.getUserByWarwickUniId(mark.universityId) match {
					case FoundUser(u) =>
					case NoUser(u) =>
						errors.rejectValue("universityId", "uniNumber.userNotFound", Array(mark.universityId), "")
						hasErrors = true
				}
				checkMarkUpdated(mark: MarkItem)
			}
		} else {
			errors.rejectValue("universityId", "NotEmpty")
			hasErrors = true
		}
		// validate mark (must be int between 0 and 100)
		if (mark.actualMark.hasText) {
			try {
				val asInt = mark.actualMark.toInt
				if (asInt < 0 || asInt > 100) {
					errors.rejectValue("actualMark", "actualMark.range")
					hasErrors = true
				}
			} catch {
				case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
					errors.rejectValue("actualMark", "actualMark.format")
					hasErrors = true
			}
		}

		// validate grade is department setting is true
		if (!hasErrors && mark.actualGrade.hasText && module.adminDepartment.assignmentGradeValidation) {
			val validGrades = gradeGenerator.applyForMarks(Map(mark.universityId -> mark.actualMark.toInt))(mark.universityId)
			if (validGrades.nonEmpty && !validGrades.exists(_.grade == mark.actualGrade)) {
				errors.rejectValue("actualGrade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
				hasErrors = true
			}
		}

		if (!mark.actualMark.hasText && !mark.actualGrade.hasText) {
			// If a row has no mark or grade, we will quietly ignore it 
			hasErrors = true
		}
		!hasErrors
	}

	override def onBind(result:BindingResult) {
		val fileNames = file.fileNames map (_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !validAttachmentStrings.exists(s.endsWith))

		if (invalidFiles.size > 0) {
			if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
			else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
		}

		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
				if (!file.attached.isEmpty) {
					processFiles(file.attached)
				}
	
				def processFiles(files: Seq[FileAttachment]) {
					for (file <- files.filter(_.hasData)) {
						try {
							marks.addAll(marksExtractor.readXSSFExcelFile(file.dataStream))
						} catch {
							case e: InvalidFormatException =>
								result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
						}
					}
				}
			}
		}
	}

	def describe(d: Description) = d.assignment(assignment)

}