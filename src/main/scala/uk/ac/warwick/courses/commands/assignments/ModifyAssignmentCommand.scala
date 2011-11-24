package uk.ac.warwick.courses.commands.assignments
import scala.reflect.BeanProperty
import org.springframework.format.annotation.DateTimeFormat
import org.hibernate.validator.constraints.NotEmpty
import org.joda.time.DateTime
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.commands.Command

abstract class ModifyAssignmentCommand extends Command[Assignment] with Daoisms  {
	
	@NotEmpty
	@BeanProperty var name:String = _
	
    @DateTimeFormat(style = "MM")
	@BeanProperty var openDate:DateTime = new DateTime().withTime(12,0,0,0)
	
    @DateTimeFormat(style = "MM")
	@BeanProperty var closeDate:DateTime = openDate.plusWeeks(2)
	
	def copyTo(assignment:Assignment) {
		assignment.name = name
	    assignment.openDate = openDate
	    assignment.closeDate = closeDate
	}
	
	def copyFrom(assignment:Assignment) {
		name = assignment.name
		openDate = assignment.openDate
		closeDate = assignment.closeDate
	}
}