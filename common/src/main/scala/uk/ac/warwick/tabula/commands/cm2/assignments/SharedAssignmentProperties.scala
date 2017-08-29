package uk.ac.warwick.tabula.commands.cm2.assignments

import javax.validation.constraints.{Max, Min}

import org.hibernate.validator.constraints.Length
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{CommentField, FileField, MarkerSelectField, WordCountField}
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipService, ZipServiceComponent}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

/**
 * Bound as the value of a Map on a parent form object, to store multiple sets of
 * assignment properties.
 *
 * Currently relies on having a default empty constructor for lazy map populating to work,
 * so if you need to add a constructor then any command that has this as a Map value will need
 * to instantiate some kind of lazy map factory that knows how to create them (see Commons Collections LazyMap).
 */
class SharedAssignmentPropertiesForm extends SharedAssignmentProperties with AutowiringZipServiceComponent {
	// set this as the default for this form, because in the AddAssignmentsCommand
	// we're linking assignments in from SITS.
	restrictSubmissions = true
}

trait SharedAssignmentDetailProperties extends BooleanAssignmentDetailProperties {
	def copySharedDetailFrom(assignment: Assignment): Unit = {
		openEnded = assignment.openEnded
	}

	def copySharedDetailTo(assignment: Assignment): Unit = {
		copyDetailBooleansTo(assignment)
	}
}

trait SharedAssignmentFeedbackProperties extends BooleanAssignmentFeedbackProperties {
	self: ZipServiceComponent =>

	// linked feedback template (optional)
	var feedbackTemplate: FeedbackTemplate = _

	def copySharedFeedbackFrom(assignment: Assignment): Unit = {
		collectMarks = assignment.collectMarks
		summative = assignment.summative
		dissertation = assignment.dissertation
		publishFeedback = assignment.publishFeedback
		feedbackTemplate = assignment.feedbackTemplate
		includeInFeedbackReportWithoutSubmissions = assignment.includeInFeedbackReportWithoutSubmissions
		automaticallyReleaseToMarkers = assignment.automaticallyReleaseToMarkers
	}

	def copySharedFeedbackTo(assignment: Assignment): Unit = {
		assignment.feedbackTemplate = feedbackTemplate

		// if we change a feedback template we may need to invalidate existing zips
		if (assignment.id != null) // this is an edit
			zipService.invalidateSubmissionZip(assignment)

		copyFeedbackBooleansTo(assignment)
	}
}

trait SharedAssignmentStudentProperties extends BooleanAssignmentStudentProperties {

	var anonymity: AssignmentAnonymity =_

	def copySharedStudentFrom(assignment: Assignment): Unit = {
		anonymity = assignment.anonymity
	}

	def copySharedStudentTo(assignment: Assignment): Unit = {
		copyStudentBooleansTo(assignment)
	}
}

trait SharedAssignmentSubmissionProperties extends BooleanAssignmentSubmissionProperties {
	def copySharedSubmissionFrom(assignment: Assignment): Unit = {
		collectSubmissions = assignment.collectSubmissions
		restrictSubmissions = assignment.restrictSubmissions
		allowLateSubmissions = assignment.allowLateSubmissions
		allowResubmission = assignment.allowResubmission
		displayPlagiarismNotice = assignment.displayPlagiarismNotice
		allowExtensions = assignment.allowExtensions
		extensionAttachmentMandatory = assignment.extensionAttachmentMandatory
		allowExtensionsAfterCloseDate = assignment.allowExtensionsAfterCloseDate
		automaticallySubmitToTurnitin = assignment.automaticallySubmitToTurnitin
	}

	def copySharedSubmissionTo(assignment: Assignment): Unit = {
		copySubmissionBooleansTo(assignment)
	}
}

trait SharedAssignmentOptionsProperties extends FindAssignmentFields {

	@Min(0)
	var wordCountMin: JInteger = _
	@Max(Assignment.MaximumWordCount)
	var wordCountMax: JInteger = _
	@Length(max = 600)
	var wordCountConventions: String = "Exclude any bibliography or appendices from your word count."

	@Min(1)
	@Max(Assignment.MaximumFileAttachments)
	var fileAttachmentLimit: Int = 1

	@Min(1)
	@Max(Assignment.MaximumFileAttachments)
	var minimumFileAttachmentLimit: Int = 1

	val maxFileAttachments: Int = 20

	val invalidAttachmentPattern = """.*[\*\\/:\?"<>\|\%].*"""

	var fileAttachmentTypes: JList[String] = JArrayList()

	@Min(0)
	var individualFileSizeLimit: JInteger = _

	/**
		* This isn't actually a property on Assignment, it's one of the default fields added
		* to all Assignments. When the forms become customisable this will be replaced with
		* a full blown field editor.
		*/
	@Length(max = 2000)
	var comment: String = _

	def copySharedOptionsFrom(assignment: Assignment): Unit = {
		for (field <- findCommentField(assignment)) comment = field.value
		for (file <- findFileField(assignment)) {
			fileAttachmentLimit = file.attachmentLimit
			minimumFileAttachmentLimit = file.minimumAttachmentLimit
			fileAttachmentTypes = file.attachmentTypes
			individualFileSizeLimit = file.individualFileSizeLimit
		}

		for (wordCount <- findWordCountField(assignment)) {
			wordCountMin = wordCount.min
			wordCountMax = wordCount.max
			wordCountConventions = wordCount.conventions
		}
	}

	def copySharedOptionsTo(assignment: Assignment): Unit = {
		for (field <- findCommentField(assignment)) field.value = comment
		for (file <- findFileField(assignment)) {
			file.attachmentLimit = fileAttachmentLimit
			file.minimumAttachmentLimit = minimumFileAttachmentLimit
			file.attachmentTypes = fileAttachmentTypes
			file.individualFileSizeLimit = individualFileSizeLimit
		}

		if (wordCountMin == null && wordCountMax == null) {
			findWordCountField(assignment).foreach { wordCountField =>
				wordCountField.max = null
				wordCountField.min = null
				wordCountField.conventions = wordCountConventions
			}
		} else {
			val wordCount = findWordCountField(assignment).getOrElse {
				val newField = new WordCountField()
				newField.name = Assignment.defaultWordCountName
				assignment.addField(newField)
				newField
			}
			wordCount.min = wordCountMin
			wordCount.max = wordCountMax
			wordCount.conventions = wordCountConventions
		}
	}

	def validateSharedOptions(errors: Errors): Unit = {
		if (fileAttachmentTypes.mkString("").matches(invalidAttachmentPattern)) {
			errors.rejectValue("fileAttachmentTypes", "attachment.invalidChars")
		}

		// implicitly fix missing bounds
		(Option(wordCountMin), Option(wordCountMax)) match {
			case (Some(min), Some(max)) if max <= min => errors.rejectValue("wordCountMax", "assignment.wordCount.outOfRange")
			case (Some(min), None) => wordCountMax = Assignment.MaximumWordCount
			case (None, Some(max)) => wordCountMin = 0
			case _ => // It's All Good
		}

		if (fileAttachmentLimit < minimumFileAttachmentLimit) errors.rejectValue("fileAttachmentLimit", "assignment.attachments.outOfRange")
	}
}

/**
 * Contains all the fields that could be collectively assigned to a group of assignments,
 * so that we can set options in one go.
 */
trait SharedAssignmentProperties
	extends SharedAssignmentDetailProperties
		with SharedAssignmentFeedbackProperties
		with SharedAssignmentStudentProperties
		with SharedAssignmentSubmissionProperties
		with SharedAssignmentOptionsProperties {
	self: ZipServiceComponent =>

	def validateShared(errors: Errors): Unit = {
		validateSharedOptions(errors)
	}

	def copySharedTo(assignment: Assignment): Unit = {
		copySharedDetailTo(assignment)
		copySharedFeedbackTo(assignment)
		copySharedStudentTo(assignment)
		copySharedSubmissionTo(assignment)
		copySharedOptionsTo(assignment)
	}

	def copySharedFrom(assignment: Assignment): Unit = {
		copySharedDetailFrom(assignment)
		copySharedFeedbackFrom(assignment)
		copySharedStudentFrom(assignment)
		copySharedSubmissionFrom(assignment)
		copySharedOptionsFrom(assignment)
	}

}

trait FindAssignmentFields {
	protected def findFileField(assignment: Assignment): Option[FileField] =
		assignment.findFieldOfType[FileField](Assignment.defaultUploadName)

	protected def findMarkerSelectField(assignment: Assignment): Option[MarkerSelectField] =
		assignment.findFieldOfType[MarkerSelectField](Assignment.defaultMarkerSelectorName)

	/**Find the standard free-text field if it exists */
	protected def findCommentField(assignment: Assignment): Option[CommentField] =
		assignment.findFieldOfType[CommentField](Assignment.defaultCommentFieldName)

	protected def findWordCountField(assignment: Assignment): Option[WordCountField] = {
		assignment.findFieldOfType[WordCountField](Assignment.defaultWordCountName)
	}
}