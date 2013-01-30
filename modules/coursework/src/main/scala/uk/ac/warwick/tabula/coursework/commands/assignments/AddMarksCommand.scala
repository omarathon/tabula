package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.util.matching.Regex
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.mutable
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.util.core.StringUtils.hasText
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.services.docconversion.MarksExtractor
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.coursework.services.docconversion.MarkItem
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.helpers.NoUser
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.UniversityId
import org.springframework.beans.factory.annotation.Value
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.permissions._


class AddMarksCommand(val module: Module, val assignment: Assignment, val submitter: CurrentUser) extends Command[List[Feedback]] with Daoisms with Logging with BindListener {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Marks.Create(), assignment)

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
				// Warn if marks for this student are already uploaded
				assignment.feedbacks.find { (feedback) => feedback.universityId == mark.universityId && (feedback.hasMark || feedback.hasGrade) } match {
					case Some(feedback) => {
						mark.warningMessage = markWarning
					}
					case None => {}
				}
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

	override def applyInternal(): List[Feedback] = transactional() {
		def saveFeedback(universityId: String, actualMark: String, actualGrade: String) = {
			val feedback = assignment.findFeedback(universityId).getOrElse(new Feedback)
			feedback.assignment = assignment
			feedback.uploaderId = submitter.apparentId
			feedback.universityId = universityId
			feedback.released = false
			if (hasText(actualMark)){
				feedback.actualMark = Option(actualMark.toInt)
			}
			if (hasText(actualGrade)){
				feedback.actualGrade = Option(actualGrade)
			}
			session.saveOrUpdate(feedback)
			feedback
		}

		// persist valid marks
		val markList = marks filter (_.isValid) map { (mark) => saveFeedback(mark.universityId, mark.actualMark, mark.actualGrade) }
		markList.toList
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