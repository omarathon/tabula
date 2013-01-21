package uk.ac.warwick.tabula.coursework.commands.markschemes

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.Errors
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MarkSchemeDao
import uk.ac.warwick.tabula.actions.Manage

class DeleteMarkSchemeCommand(val department: Department, val markScheme: MarkScheme) extends Command[Unit] with SelfValidating {
	
	mustBeLinked(markScheme, department)
	PermissionsCheck(Manage(department))
	
	var dao = Wire.auto[MarkSchemeDao]
	
	override def applyInternal() {
		transactional() {
			department.markSchemes.remove(markScheme)
		}
	}
	
	def validate(errors: Errors) {
		// can't delete a mark scheme that's being referenced by assignments.
		if (!dao.getAssignmentsUsingMarkScheme(markScheme).isEmpty()) {
			errors.reject("markScheme.inuse")
		}
	}
	
	override def describe(d: Description) {
		
	}
	
}