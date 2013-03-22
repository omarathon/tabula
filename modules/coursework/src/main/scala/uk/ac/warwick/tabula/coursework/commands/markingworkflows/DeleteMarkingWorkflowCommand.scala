package uk.ac.warwick.tabula.coursework.commands.markingworkflows

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.Errors
import scala.beans.BeanProperty
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MarkingWorkflowDao
import uk.ac.warwick.tabula.permissions._

class DeleteMarkingWorkflowCommand(val department: Department, val markingWorkflow: MarkingWorkflow) extends Command[Unit] with SelfValidating {
	
	mustBeLinked(markingWorkflow, department)
	PermissionCheck(Permissions.MarkingWorkflow.Delete, markingWorkflow)
	
	var dao = Wire.auto[MarkingWorkflowDao]
	
	override def applyInternal() {
		transactional() {
			department.markingWorkflows.remove(markingWorkflow)
		}
	}
	
	def validate(errors: Errors) {
		// can't delete a markingWorkflow that's being referenced by assignments.
		if (!dao.getAssignmentsUsingMarkingWorkflow(markingWorkflow).isEmpty) {
			errors.reject("markingWorkflow.inuse")
		}
	}
	
	override def describe(d: Description) {
		
	}
	
}