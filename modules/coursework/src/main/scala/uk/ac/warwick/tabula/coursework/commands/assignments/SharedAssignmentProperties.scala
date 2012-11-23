package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import reflect.BeanProperty
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.JavaImports._
import javax.validation.constraints.{ Max, Min }
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.ArrayList
import org.hibernate.validator.constraints.Length
import uk.ac.warwick.tabula.data.model.forms.{MarkerSelectField, CommentField, FileField, WordCountField }
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire

/**
 * Bound as the value of a Map on a parent form object, to store multiple sets of
 * assignment properties.
 *
 * Currently relies on having a default empty constructor for lazy map populating to work,
 * so if you need to add a constructor then any command that has this as a Map value will need
 * to instantiate some kind of lazy map factory that knows how to create them (see Commons Collections LazyMap).
 */
class SharedAssignmentPropertiesForm extends SharedAssignmentProperties {
	// set this as the default for this form, because in the AddAssignmentsCommand
	// we're linking assignments in from SITS.
	restrictSubmissions = true
}

/**
 * Contains all the fields that could be collectively assigned to a group of assignments, so that
 * we can set options for a group of assignments all in one go.
 */
trait SharedAssignmentProperties {


	@BeanProperty var openEnded: JBoolean = false
	@BeanProperty var collectMarks: JBoolean = false
	@BeanProperty var collectSubmissions: JBoolean = false
	@BeanProperty var restrictSubmissions: JBoolean = false
	@BeanProperty var allowLateSubmissions: JBoolean = true
	@BeanProperty var allowResubmission: JBoolean = false
	@BeanProperty var displayPlagiarismNotice: JBoolean = false
	@BeanProperty var allowExtensions: JBoolean = false
	@BeanProperty var allowExtensionRequests: JBoolean = false
	
	@Min(0)
	@BeanProperty var wordCountMin: JInteger = _
	@Max(Assignment.MaximumWordCount)
	@BeanProperty var wordCountMax: JInteger = _
	@Length(max = 500)
	@BeanProperty var wordCountConventions: String = "Exclude any bibliography or appendices."
	
	// linked feedback template (optional)
	@BeanProperty var feedbackTemplate: FeedbackTemplate = _
	// if we change a feedback template we may need to invalidate existing zips
	var zipService: ZipService = Wire.auto[ZipService]
	@BeanProperty var markScheme: MarkScheme = _

	@Min(1)
	@Max(Assignment.MaximumFileAttachments)
	@BeanProperty var fileAttachmentLimit: Int = 1

	@BeanProperty val maxFileAttachments: Int = 10

	val invalidAttachmentPattern = """.*[\*\\/:\?"<>\|\%].*"""

	@BeanProperty var fileAttachmentTypes: JList[String] = ArrayList()

	/**
	 * This isn't actually a property on Assignment, it's one of the default fields added
	 * to all Assignments. When the forms become customisable this will be replaced with
	 * a full blown field editor.
	 */
	@Length(max = 2000)
	@BeanProperty var comment: String = _
	
	def validateShared(errors: Errors) {
		if(fileAttachmentTypes.mkString("").matches(invalidAttachmentPattern)){
			errors.rejectValue("fileAttachmentTypes", "attachment.invalidChars")
		}
		
		// implicitly fix missing bounds
		Pair(Option(wordCountMin), Option(wordCountMax)) match {
			case (Some(min), Some(max)) if (max <= min) => errors.rejectValue("wordCountMax", "assignment.wordCount.outOfRange")
			case (Some(min), None) => wordCountMax = Assignment.MaximumWordCount
			case (None, Some(max)) => wordCountMin = 0
			case _ => // It's All Good
		}
	}

	def copySharedTo(assignment: Assignment) {
		assignment.openEnded = openEnded
		assignment.collectMarks = collectMarks
		assignment.collectSubmissions = collectSubmissions
		assignment.restrictSubmissions = restrictSubmissions
		assignment.allowLateSubmissions = allowLateSubmissions
		assignment.allowResubmission = allowResubmission
		assignment.displayPlagiarismNotice = displayPlagiarismNotice
		assignment.allowExtensions = allowExtensions
		assignment.allowExtensionRequests = allowExtensionRequests
		assignment.feedbackTemplate = feedbackTemplate
		if (assignment.id != null) // this is an edit
			zipService.invalidateSubmissionZip(assignment)
		assignment.markScheme = markScheme
		manageMarkerField(assignment)

		for (field <- findCommentField(assignment)) field.value = comment
		for (file <- findFileField(assignment)) {
			file.attachmentLimit = fileAttachmentLimit
			file.attachmentTypes = fileAttachmentTypes
		}
		
		val wordCount = findWordCountField(assignment)
		if (wordCountMax != null) {
			wordCount.min = wordCountMin
			wordCount.max = wordCountMax
			wordCount.conventions = wordCountConventions
		} else {
			// none set, so remove field
			assignment.removeField(wordCount)
		}
	}

	def copySharedFrom(assignment: Assignment) {
		openEnded = assignment.openEnded
		collectMarks = assignment.collectMarks
		collectSubmissions = assignment.collectSubmissions
		restrictSubmissions = assignment.restrictSubmissions
		allowLateSubmissions = assignment.allowLateSubmissions
		allowResubmission = assignment.allowResubmission
		displayPlagiarismNotice = assignment.displayPlagiarismNotice
		allowExtensions = assignment.allowExtensions
		allowExtensionRequests = assignment.allowExtensionRequests
		feedbackTemplate = assignment.feedbackTemplate
		markScheme = assignment.markScheme
		
		for (field <- findCommentField(assignment)) comment = field.value
		for (file <- findFileField(assignment)) {
			fileAttachmentLimit = file.attachmentLimit
			fileAttachmentTypes = file.attachmentTypes
		}
		
		val wordCount = findWordCountField(assignment)
		wordCountMin = wordCount.min
		wordCountMax = wordCount.max
		wordCountConventions = wordCount.conventions
	}
	
	/**
	 * add/remove marker field as appropriate
 	 */
	def manageMarkerField(assignment:Assignment) {
		val markerField = findMarkerSelectField(assignment)
		if (markScheme != null && markScheme.studentsChooseMarker){
			// we now need a marker field for this assignment. create one
			if (!markerField.isDefined) {
				val markerSelect = new MarkerSelectField()
				markerSelect.name = Assignment.defaultMarkerSelectorName
				assignment.addFields(markerSelect)
			}
		} else {
			// if a mark scheme has been removed or changed we need to remove redundant marker fields
			if (markerField.isDefined) {
				assignment.removeField(markerField.get)
			}
		}
	}

	protected def findFileField(assignment: Assignment) =
		assignment.findFieldOfType[FileField](Assignment.defaultUploadName)

	protected def findMarkerSelectField(assignment: Assignment) =
		assignment.findFieldOfType[MarkerSelectField](Assignment.defaultMarkerSelectorName)

	/**Find the standard free-text field if it exists */
	protected def findCommentField(assignment: Assignment) =
		assignment.findFieldOfType[CommentField](Assignment.defaultCommentFieldName)

	protected def findWordCountField(assignment: Assignment) = {
		assignment.findFieldOfType[WordCountField](Assignment.defaultWordCountName) getOrElse({
			val newField = new WordCountField()
			newField.name = Assignment.defaultWordCountName
			assignment.addField(newField)
			newField
		})
	}
}
