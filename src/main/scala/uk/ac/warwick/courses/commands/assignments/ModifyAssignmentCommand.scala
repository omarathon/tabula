package uk.ac.warwick.courses.commands.assignments
import scala.reflect.BeanProperty
import org.hibernate.validator.constraints.NotEmpty
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.AcademicYear

abstract class ModifyAssignmentCommand extends Command[Assignment]  {
	
	@Autowired var service:AssignmentService =_
	
	@NotEmpty(message="{NotEmpty.assignmentName}")
	@BeanProperty var name:String = _
	
    @DateTimeFormat(style = "MM")
	@BeanProperty var openDate:DateTime = new DateTime().withTime(12,0,0,0)
	
    @DateTimeFormat(style = "MM")
	@BeanProperty var closeDate:DateTime = openDate.plusWeeks(2)
	
	@BeanProperty var academicYear:AcademicYear = AcademicYear.guessByDate(new DateTime)
	
	def getAcademicYearString = if (academicYear != null) academicYear.toString() else ""
	
	@BeanProperty var collectMarks:Boolean = _
	@BeanProperty var collectSubmissions:Boolean = _
	@BeanProperty var restrictSubmissions:Boolean = _
	@BeanProperty var allowLateSubmissions:Boolean = _
	
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
	}
}