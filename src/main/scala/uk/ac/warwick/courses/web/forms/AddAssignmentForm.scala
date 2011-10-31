package uk.ac.warwick.courses.web.forms;

import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.validator.constraints.NotEmpty
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import javax.persistence.Entity
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.validators.SpelAssert.List
import uk.ac.warwick.courses.validators.SpelAssert
import uk.ac.warwick.courses.data.model.Module

@SpelAssert.List(Array(
	new SpelAssert(value="openDate < closeDate", message="{closeDate.early}")
))
class AddAssignmentForm(val module:Module) {
    
	@NotEmpty
	@BeanProperty var name:String = _
	
    @DateTimeFormat(style = "MM")
	@BeanProperty var openDate:DateTime = new DateTime().withTime(12,0,0,0)
	
    @DateTimeFormat(style = "MM")
	@BeanProperty var closeDate:DateTime = openDate.plusWeeks(2)
	
	def createAssignment:Assignment = {
	  val assignment = new Assignment(module)
	  assignment.name = name
	  assignment.openDate = openDate
	  assignment.closeDate = closeDate
	  assignment.active = true
	  assignment
	}
	
}
