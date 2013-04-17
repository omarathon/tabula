package uk.ac.warwick.tabula.coursework.commands.markingworkflows

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._

class AddMarkingWorkflowCommand(department: Department) extends ModifyMarkingWorkflowCommand(department) {
	
	PermissionCheck(Permissions.MarkingWorkflow.Create, department)

	// Copy properties to a new MarkingWorkflow, save it transactionally, return it.
	def applyInternal() = {
		transactional() { 
			val markingWorkflow = new MarkingWorkflow(department)
			this.copyTo(markingWorkflow)
			session.save(markingWorkflow)
			markingWorkflow
		}
	}
	
	// For validation. Not editing an existing MarkingWorkflow so return None
	def currentMarkingWorkflow = None

	def contextSpecificValidation(errors:Errors){}

	override def validate(errors: Errors) {
		super.validate(errors)
	}
	

	def describe(d: Description) = d.department(department)
}