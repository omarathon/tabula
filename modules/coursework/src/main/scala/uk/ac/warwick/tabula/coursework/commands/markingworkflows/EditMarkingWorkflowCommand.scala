package uk.ac.warwick.tabula.coursework.commands.markingworkflows

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import reflect.BeanProperty
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MarkingWorkflowDao
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.AssignmentServiceComponent
import uk.ac.warwick.tabula.services.MarkingWorkflowServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.services.AutowiringMarkingWorkflowServiceComponent
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking

object EditMarkingWorkflowCommand {
	def apply(department: Department, markingWorkflow: MarkingWorkflow) =
		new EditMarkingWorkflowCommandInternal(department, markingWorkflow)
			with ComposableCommand[MarkingWorkflow]
			with EditMarkingWorkflowCommandPermissions
			with EditMarkingWorkflowCommandValidation
			with EditMarkingWorkflowCommandDescription
			with AutowiringMarkingWorkflowServiceComponent
}

/** Edit an existing markingWorkflow. */
class EditMarkingWorkflowCommandInternal(department: Department, val markingWorkflow: MarkingWorkflow) 
	extends ModifyMarkingWorkflowCommand(department) with EditMarkingWorkflowCommandState {
	self: MarkingWorkflowServiceComponent =>	

	// fill in the properties on construction
	copyFrom(markingWorkflow)

	def applyInternal() = {
		transactional() {
			this.copyTo(markingWorkflow)
			markingWorkflowService.save(markingWorkflow)
			markingWorkflow
		}
	}
}

trait EditMarkingWorkflowCommandState extends MarkingWorkflowCommandState {
	def markingWorkflow: MarkingWorkflow
}

trait EditMarkingWorkflowCommandValidation extends MarkingWorkflowCommandValidation {
	self: EditMarkingWorkflowCommandState with MarkingWorkflowServiceComponent =>
		
	def currentMarkingWorkflow = Some(markingWorkflow)
	
	def hasExistingSubmissions: Boolean = markingWorkflowService.getAssignmentsUsingMarkingWorkflow(markingWorkflow).exists(!_.submissions.isEmpty)

	def contextSpecificValidation(errors:Errors){
		if (markingWorkflow.markingMethod != markingMethod)
			errors.rejectValue("markingMethod", "markingWorkflow.markingMethod.cannotUpdate")

		if (hasExistingSubmissions){
			if (markingWorkflow.studentsChooseMarker){
				val existingFirstMarkers = markingWorkflow.firstMarkers.includeUsers.toSet
				val newFirstMarkers = firstMarkers.toSet
				val existingSecondMarkers = markingWorkflow.secondMarkers.includeUsers.toSet
				val newSecondMarkers = secondMarkers.toSet
				// if newMarkers is not a super set of existingMarker, markers have been removed.
				if (!(existingFirstMarkers -- newFirstMarkers).isEmpty || !(existingSecondMarkers -- newSecondMarkers).isEmpty) {
					errors.rejectValue("firstMarkers", "markingWorkflow.firstMarkers.cannotRemoveMarkers")
				}
			}
		}
	}
}

trait EditMarkingWorkflowCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditMarkingWorkflowCommandState =>
		
	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(markingWorkflow), mandatory(department))
		p.PermissionCheck(Permissions.MarkingWorkflow.Update, markingWorkflow)
	}
}

trait EditMarkingWorkflowCommandDescription extends Describable[MarkingWorkflow] {
	self: EditMarkingWorkflowCommandState =>
		
	def describe(d: Description) {
		d.department(department).markingWorkflow(markingWorkflow)
	}
}