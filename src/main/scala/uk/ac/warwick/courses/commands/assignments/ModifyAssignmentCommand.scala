package uk.ac.warwick.courses.commands.assignments

import scala.reflect.BeanProperty
import scala.collection.JavaConversions._

import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.NotEmpty
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.Errors

import javax.validation.constraints.Max
import javax.validation.constraints.Min
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.data.model.forms.CommentField
import uk.ac.warwick.courses.data.model.forms.FileField
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.helpers.ArrayList
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.AcademicYear


object DateFormats {
	final val DateTimePicker = "dd-MMM-yyyy HH:mm:ss"
}

abstract class ModifyAssignmentCommand extends Command[Assignment]  {
	
	@Autowired var service:AssignmentService =_
	
	def module:Module
	def assignment:Assignment
	
	@Length(max=200)
	@NotEmpty(message="{NotEmpty.assignmentName}")
	@BeanProperty var name:String = _
	
    @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var openDate:DateTime = new DateTime().withTime(12,0,0,0)
	
    @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var closeDate:DateTime = openDate.plusWeeks(2)
	
	@BeanProperty var academicYear:AcademicYear = AcademicYear.guessByDate(new DateTime)
	
	def getAcademicYearString = if (academicYear != null) academicYear.toString() else ""
	
	@BeanProperty var collectMarks:Boolean = _
	@BeanProperty var collectSubmissions:Boolean = _
	@BeanProperty var restrictSubmissions:Boolean = _
	@BeanProperty var allowLateSubmissions:Boolean = true
	@BeanProperty var allowResubmission:Boolean = false
	
	@Min(1) @Max(Assignment.MaximumFileAttachments)
	@BeanProperty var fileAttachmentLimit:Int = 1
	
	val maxFileAttachments:Int = 10
	
	@BeanProperty var fileAttachmentTypes: JList[String] = ArrayList()
	
	/**
	 * This isn't actually a property on Assignment, it's one of the default fields added
	 * to all Assignments. When the forms become customisable this will be replaced with
	 * a full blown field editor. 
	 */
	@Length(max=2000)
	@BeanProperty var comment:String = _ 
	
	def validate(errors:Errors) {
		service.getAssignmentByNameYearModule(name, academicYear, module)
			.filterNot{ _ eq assignment }
			.map{ a => errors.rejectValue("name", "name.duplicate.assignment", Array(name), "") }
	}
	
	def copyTo(assignment:Assignment) {
		assignment.name = name
	    assignment.openDate = openDate
	    assignment.closeDate = closeDate
	    assignment.collectMarks = collectMarks
	    assignment.academicYear = academicYear
	    assignment.collectSubmissions = collectSubmissions
	    // changes disabled for now
	    //assignment.restrictSubmissions = restrictSubmissions
	    assignment.allowLateSubmissions = allowLateSubmissions
	    assignment.allowResubmission = allowResubmission
	    findCommentField(assignment) map ( field => field.value = comment )
	    findFileField(assignment) map { file => 
	    	file.attachmentLimit = fileAttachmentLimit 
	    	file.attachmentTypes = fileAttachmentTypes
		}
	}
	
	def copyFrom(assignment:Assignment) {
		name = assignment.name
		openDate = assignment.openDate
		closeDate = assignment.closeDate
		collectMarks = assignment.collectMarks
		academicYear = assignment.academicYear
		collectSubmissions = assignment.collectSubmissions
		restrictSubmissions = assignment.restrictSubmissions
		allowLateSubmissions = assignment.allowLateSubmissions
		allowResubmission = assignment.allowResubmission
		findCommentField(assignment) map ( field => comment = field.value )
		findFileField(assignment) map { field => 
			fileAttachmentLimit = field.attachmentLimit 
			fileAttachmentTypes = field.attachmentTypes
		}
	}
	
	private def findFileField(assignment:Assignment) = 
		assignment.findFieldOfType[FileField](Assignment.defaultUploadName)
	
	/** Find the standard free-text field if it exists */
	private def findCommentField(assignment:Assignment) = 
		assignment.findFieldOfType[CommentField](Assignment.defaultCommentFieldName)
}