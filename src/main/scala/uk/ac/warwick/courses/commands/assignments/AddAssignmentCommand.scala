package uk.ac.warwick.courses.commands.assignments

import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.validator.constraints.NotEmpty
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.format.annotation.DateTimeFormat
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.helpers._
import uk.ac.warwick.courses.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.courses.data.Transactions._

@Configurable
class AddAssignmentCommand(val module: Module = null) extends ModifyAssignmentCommand {

	openDate = new DateTime().withTime(12, 0, 0, 0)
	closeDate = openDate.plusWeeks(2)

	def assignment: Assignment = null

	override def work: Assignment = transactional() {
		val assignment = new Assignment(module)
		assignment.addDefaultFields()
		copyTo(assignment)
		service.save(assignment)
		assignment
	}

	override def describeResult(d: Description, assignment: Assignment) = d.assignment(assignment)

	override def describe(d: Description) = d.module(module).properties(
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate)

}
