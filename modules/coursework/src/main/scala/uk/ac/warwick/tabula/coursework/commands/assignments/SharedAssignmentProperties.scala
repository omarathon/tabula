package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import reflect.BeanProperty
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.JavaImports._
import javax.validation.constraints.{ Max, Min }
import uk.ac.warwick.tabula.data.model._
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
trait SharedAssignmentProperties extends FindAssignmentFields {


	var openEnded: JBoolean = false
	var collectMarks: JBoolean = false
	var collectSubmissions: JBoolean = true
	var restrictSubmissions: JBoolean = false
	var allowLateSubmissions: JBoolean = true
	var allowResubmission: JBoolean = false
	var displayPlagiarismNotice: JBoolean = false
	var allowExtensions: JBoolean = false
	var allowExtensionRequests: JBoolean = false
	var summative: JBoolean = true
	
	@Min(0)
	var wordCountMin: JInteger = _
	@Max(Assignment.MaximumWordCount)
	var wordCountMax: JInteger = _
	@Length(max = 600)
	var wordCountConventions: String = "Exclude any bibliography or appendices."
	
	// linked feedback template (optional)
	var feedbackTemplate: FeedbackTemplate = _
	// if we change a feedback template we may need to invalidate existing zips
	var zipService: ZipService = Wire.auto[ZipService]

	var markingWorkflow: MarkingWorkflow = _

	@Min(1)
	@Max(Assignment.MaximumFileAttachments)
	var fileAttachmentLimit: Int = 1

	val maxFileAttachments: Int = 10

	val invalidAttachmentPattern = """.*[\*\\/:\?"<>\|\%].*"""

	var fileAttachmentTypes: JList[String] = JArrayList()

	/**
	 * This isn't actually a property on Assignment, it's one of the default fields added
	 * to all Assignments. When the forms become customisable this will be replaced with
	 * a full blown field editor.
	 */
	@Length(max = 2000)
	var comment: String = _
	
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
		assignment.summative = summative
		
		assignment.feedbackTemplate = feedbackTemplate
		if (assignment.id != null) // this is an edit
			zipService.invalidateSubmissionZip(assignment)
		assignment.markingWorkflow = markingWorkflow
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
		summative = assignment.summative
		feedbackTemplate = assignment.feedbackTemplate
		markingWorkflow = assignment.markingWorkflow
		
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
		if (markingWorkflow != null && markingWorkflow.studentsChooseMarker){
			// we now need a marker field for this assignment. create one
			if (!markerField.isDefined) {
				val markerSelect = new MarkerSelectField()
				markerSelect.name = Assignment.defaultMarkerSelectorName
				assignment.addFields(markerSelect)
			}
		} else {
			// if a marking workflow has been removed or changed we need to remove redundant marker fields
			if (markerField.isDefined) {
				assignment.removeField(markerField.get)
			}
		}
	}
}

trait FindAssignmentFields {

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
