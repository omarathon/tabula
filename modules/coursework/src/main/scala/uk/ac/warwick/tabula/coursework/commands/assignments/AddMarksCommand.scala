package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import scala.collection.mutable
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.{Feedback, Assignment, Module, FileAttachment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.services.docconversion.MarksExtractor
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.NoUser
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.BindingResult
import org.apache.poi.openxml4j.exceptions.InvalidFormatException


abstract class AddMarksCommand[A](val module: Module, val assignment: Assignment, val submitter: CurrentUser) extends Command[A]
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
					case NoUser(u) => {
						errors.rejectValue("universityId", "uniNumber.userNotFound", Array(mark.universityId), "")
						hasErrors = true
					}
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
				case _ @ (_: NumberFormatException | _: IllegalArgumentException) => {
					errors.rejectValue("actualMark", "actualMark.format")
					hasErrors = true
				}
			}
		} else if (!mark.actualGrade.hasText) {
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
				if (!file.attached.isEmpty()) {
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