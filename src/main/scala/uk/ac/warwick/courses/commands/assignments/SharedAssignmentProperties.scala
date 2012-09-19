package uk.ac.warwick.courses.commands.assignments

import collection.JavaConversions._
import reflect.BeanProperty
import uk.ac.warwick.courses
import uk.ac.warwick.courses.JavaImports._
import javax.validation.constraints.{ Max, Min }
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.helpers.ArrayList
import org.hibernate.validator.constraints.Length
import uk.ac.warwick.courses.data.model.forms.{ CommentField, FileField }
import org.springframework.validation.Errors

/**
 * Bound as the value of a Map on a parent form object, to store multiple sets of
 * assignment properties.
 *
 * Currently relies on having a default empty constructor for lazy map populating to work,
 * so if you need to add a constructor then any command that has this as a Map value will need
 * to instantiate some kind of lazy map factory that knows how to create them (see Commons Collections LazyMap).
 */
class SharedAssignmentPropertiesForm extends SharedAssignmentProperties

/**
 * Contains all the fields that could be collectively assigned to a group of assignments, so that
 * we can set options for a group of assignments all in one go.
 */
trait SharedAssignmentProperties {

	@BeanProperty var collectMarks: JBoolean = false
	@BeanProperty var collectSubmissions: JBoolean = false
	@BeanProperty var restrictSubmissions: JBoolean = false
	@BeanProperty var allowLateSubmissions: JBoolean = true
	@BeanProperty var allowResubmission: JBoolean = false
	@BeanProperty var displayPlagiarismNotice: JBoolean = false
	@BeanProperty var allowExtensions: JBoolean = false
	@BeanProperty var allowExtensionRequests: JBoolean = false

	@Min(1)
	@Max(Assignment.MaximumFileAttachments)
	@BeanProperty var fileAttachmentLimit: Int = 1

	@BeanProperty val maxFileAttachments: Int = 10

	val invalidAttachmentPattern = """.*[\*\\/:\?"<>\|\%].*""";

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
	}

	def copySharedTo(assignment: Assignment) {
		assignment.collectMarks = collectMarks
		assignment.collectSubmissions = collectSubmissions
		assignment.restrictSubmissions = restrictSubmissions
		assignment.allowLateSubmissions = allowLateSubmissions
		assignment.allowResubmission = allowResubmission
		assignment.displayPlagiarismNotice = displayPlagiarismNotice
		assignment.allowExtensions = allowExtensions
		assignment.allowExtensionRequests = allowExtensionRequests

		for (field <- findCommentField(assignment)) field.value = comment
		for (file <- findFileField(assignment)) {
			file.attachmentLimit = fileAttachmentLimit
			file.attachmentTypes = fileAttachmentTypes
		}
	}

	def copySharedFrom(assignment: Assignment) {
		collectMarks = assignment.collectMarks
		collectSubmissions = assignment.collectSubmissions
		restrictSubmissions = assignment.restrictSubmissions
		allowLateSubmissions = assignment.allowLateSubmissions
		allowResubmission = assignment.allowResubmission
		displayPlagiarismNotice = assignment.displayPlagiarismNotice
		allowExtensions = assignment.allowExtensions
		allowExtensionRequests = assignment.allowExtensionRequests

		for (field <- findCommentField(assignment)) comment = field.value
		for (file <- findFileField(assignment)) {
			fileAttachmentLimit = file.attachmentLimit
			fileAttachmentTypes = file.attachmentTypes
		}
	}

	protected def findFileField(assignment: Assignment) =
		assignment.findFieldOfType[FileField](Assignment.defaultUploadName)

	/**Find the standard free-text field if it exists */
	protected def findCommentField(assignment: Assignment) =
		assignment.findFieldOfType[CommentField](Assignment.defaultCommentFieldName)
}
