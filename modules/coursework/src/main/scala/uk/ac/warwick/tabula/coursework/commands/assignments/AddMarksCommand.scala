package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.mutable
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.util.core.StringUtils.hasText
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


abstract class AddMarksCommand[T](val module: Module, val assignment: Assignment, val submitter: CurrentUser) extends Command[T]
	with Daoisms with Logging with BindListener {

	var userLookup = Wire.auto[UserLookupService]
	var marksExtractor = Wire.auto[MarksExtractor]

	var markWarning = Wire.property("${mark.warning}")
  
	@BeanProperty var file: UploadedFile = new UploadedFile
	@BeanProperty var marks: JList[MarkItem] = LazyLists.simpleFactory()

	private def filenameOf(path: String) = new java.io.File(path).getName

	def postExtractValidation(errors: Errors) = {
		val uniIdsSoFar: mutable.Set[String] = mutable.Set()

		if (marks != null && !marks.isEmpty()) {
			for (i <- 0 until marks.length) {
				val mark = marks.get(i)
				val newPerson = uniIdsSoFar.add(mark.universityId)
				errors.pushNestedPath("marks[" + i + "]")
				mark.isValid = validateMarkItem(mark, errors, newPerson)
				errors.popNestedPath()
			}
		}
	}

	def checkIfDuplicate(mark: MarkItem)

	def validateMarkItem(mark: MarkItem, errors: Errors, newPerson: Boolean) = {

		var noErrors = true
		// validate id
		if (hasText(mark.universityId)) {
			if (!UniversityId.isValid(mark.universityId)) {
				errors.rejectValue("universityId", "uniNumber.invalid")
				noErrors = false
			} else if (!newPerson) {
				errors.rejectValue("universityId", "uniNumber.duplicate.mark")
				noErrors = false
			} else {
				userLookup.getUserByWarwickUniId(mark.universityId) match {
					case FoundUser(u) =>
					case NoUser(u) => {
						errors.rejectValue("universityId", "uniNumber.userNotFound", Array(mark.universityId), "")
						noErrors = false
					}
				}
				checkIfDuplicate(mark: MarkItem)
			}
		} else {
			errors.rejectValue("universityId", "NotEmpty")
		}
		// validate mark (must be int between 0 and 100)
		if (hasText(mark.actualMark)) {
			try {
				val asInt = mark.actualMark.toInt
				if (asInt < 0 || asInt > 100) {
					errors.rejectValue("actualMark", "actualMark.range")
					noErrors = false
				}
			} catch {
				case _ => {
					errors.rejectValue("actualMark", "actualMark.format")
					noErrors = false
				}
			}
		} else if (!hasText(mark.actualGrade)) {
			// If a row has no mark or grade, we will quietly ignore it 
			noErrors = false
		}
		noErrors
	}

	override def onBind {
		transactional() {
			file.onBind
			if (!file.attached.isEmpty()) {
				processFiles(file.attached)
			}

			def processFiles(files: Seq[FileAttachment]) {
				for (file <- files.filter(_.hasData)) {
					marks addAll marksExtractor.readXSSFExcelFile(file.dataStream)
				}
			}
		}
	}

	def describe(d: Description) = d.assignment(assignment)

}