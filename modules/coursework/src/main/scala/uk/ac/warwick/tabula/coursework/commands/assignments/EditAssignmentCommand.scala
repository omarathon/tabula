package uk.ac.warwick.tabula.coursework.commands.assignments;

import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.validator.constraints.NotEmpty
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.data.Transactions._
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.data.model.Module
import uk.ac.warwick.tabula.coursework.data.Daoisms
import uk.ac.warwick.tabula.coursework.commands.Command
import uk.ac.warwick.tabula.coursework.commands.Description

class EditAssignmentCommand(val assignment: Assignment = null) extends ModifyAssignmentCommand {

	this.copyFrom(assignment)

	def module = assignment.module

	override def work: Assignment = transactional() {
		copyTo(assignment)
		service.save(assignment)
		assignment
	}

	override def describe(d: Description) = d.assignment(assignment).properties(
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate)

}
