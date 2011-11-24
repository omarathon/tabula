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

@SpelAssert.List(Array(
	new SpelAssert(value="openDate < closeDate", message="{closeDate.early}")
))
@Configurable
class AddAssignmentCommand(val module:Module=null) extends ModifyAssignmentCommand {
	
	openDate = new DateTime().withTime(12,0,0,0)
    closeDate = openDate.plusWeeks(2)
	
	@Transactional
	override def apply:Assignment = {
	  val assignment = new Assignment(module)
	  copyTo(assignment)
	  assignment.active = false
	  session.save(assignment)
	  assignment
	}
	
	override def describe(d:Description) = d.properties(
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate
	)
	
}
