package uk.ac.warwick.courses.commands.assignments;

import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.validator.constraints.NotEmpty
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.validators.SpelAssert.List
import uk.ac.warwick.courses.validators.SpelAssert
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.events.EventName

@SpelAssert.List(Array(
	new SpelAssert(value="openDate < closeDate", message="{closeDate.early}")
))
@Configurable
@EventName("AddAssignment")
class AddAssignmentCommand(val module:Module=null) extends Command[Assignment] with Daoisms {
	
	@NotEmpty
	@BeanProperty var name:String = _
	
    @DateTimeFormat(style = "MM")
	@BeanProperty var openDate:DateTime = new DateTime().withTime(12,0,0,0)
	
    @DateTimeFormat(style = "MM")
	@BeanProperty var closeDate:DateTime = openDate.plusWeeks(2)
	
	@Transactional
	def apply:Assignment = {
	  val assignment = new Assignment(module)
	  assignment.name = name
	  assignment.openDate = openDate
	  assignment.closeDate = closeDate
	  assignment.active = true
	  session.save(assignment)
	  assignment
	}
	
	def describe(d:Description) = d.properties(
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate
	)
	
}
