package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.validator.constraints.NotEmpty
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.format.annotation.DateTimeFormat
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers._
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.actions.Participate


class AddAssignmentCommand(val module: Module = null) extends ModifyAssignmentCommand {
	
	PermissionsCheck(Participate(module))

	openDate = new DateTime().withTime(12, 0, 0, 0)
	closeDate = openDate.plusWeeks(2)

	def assignment: Assignment = null

	override def applyInternal(): Assignment = transactional() {
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

	// can be overridden in concrete implementations to provide additional validation
	def contextSpecificValidation(errors: Errors) {}
}
