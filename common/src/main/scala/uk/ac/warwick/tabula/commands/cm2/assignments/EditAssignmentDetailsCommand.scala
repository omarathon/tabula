package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{CreatesMarkingWorkflow, EditMarkingWorkflowState, ModifyMarkingWorkflowState, ModifyMarkingWorkflowValidation}
import uk.ac.warwick.tabula.data.model.{WorkflowCategory, _}
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, UserLookupComponent, _}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._


object EditAssignmentDetailsCommand {
	def apply(assignment: Assignment) =
		new EditAssignmentDetailsCommandInternal(assignment)
			with ComposableCommand[Assignment]
			with BooleanAssignmentDetailProperties
			with EditAssignmentPermissions
			with EditAssignmentDetailsDescription
			with EditAssignmentDetailsValidation
			with ModifyAssignmentScheduledNotifications
			with AutowiringAssessmentServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringCM2MarkingWorkflowServiceComponent
			with ModifyAssignmentsDetailsTriggers
			with PopulateOnForm
}

class EditAssignmentDetailsCommandInternal(override val assignment: Assignment) extends CommandInternal[Assignment] with EditAssignmentDetailsCommandState
	with EditAssignmentDetailsValidation with SharedAssignmentDetailProperties with PopulateOnForm with AssignmentDetailsCopy with CreatesMarkingWorkflow {

	self: AssessmentServiceComponent with UserLookupComponent with CM2MarkingWorkflowServiceComponent =>

	override def applyInternal(): Assignment = {

		if(workflowCategory == WorkflowCategory.SingleUse) {
			workflow.filterNot(_.isReusable) match {
				// update any existing single use workflows
				case Some(w) =>
					if(workflowType != null && w.workflowType != workflowType){
						// delete the old workflow and make a new one with the new type
						cm2MarkingWorkflowService.delete(w)
						createAndSaveSingleUseWorkflow(assignment)
					} else {
						w.replaceMarkers(markersAUsers, markersBUsers)
						cm2MarkingWorkflowService.save(w)
					}
				// persist any new workflows
				case _ => createAndSaveSingleUseWorkflow(assignment)
			}
		} else if(workflowCategory == WorkflowCategory.NoneUse || workflowCategory == WorkflowCategory.NotDecided) {
			// before we de-attach, store it to be deleted afterwards
			val existingWorkflow = workflow
			assignment.cm2MarkingWorkflow = null
			existingWorkflow.filterNot(_.isReusable).foreach(cm2MarkingWorkflowService.delete)
		} else if(workflowCategory == WorkflowCategory.Reusable) {
			workflow.filterNot(_.isReusable).foreach(cm2MarkingWorkflowService.delete)
		}

		copyTo(assignment)

		assessmentService.save(assignment)
		assignment
	}

	override def populate(): Unit = {
		name = assignment.name
		openDate = assignment.openDate
		openEnded = assignment.openEnded
		openEndedReminderDate = assignment.openEndedReminderDate
		closeDate = assignment.closeDate
		workflowCategory = assignment.workflowCategory.getOrElse(WorkflowCategory.NotDecided)
		reusableWorkflow = Option(assignment.cm2MarkingWorkflow).filter(_.isReusable).orNull
		workflow.foreach(w => workflowType = w.workflowType)
		extractMarkers match { case (a, b) =>
			markersA = JArrayList(a)
			markersB = JArrayList(b)
		}
	}

}


trait EditAssignmentDetailsCommandState extends ModifyAssignmentDetailsCommandState with EditMarkingWorkflowState {

	self: AssessmentServiceComponent with UserLookupComponent with CM2MarkingWorkflowServiceComponent =>

	def assignment: Assignment
	def academicYear: AcademicYear = assignment.academicYear
	def module: Module = assignment.module
	def workflow: Option[CM2MarkingWorkflow] = Option(assignment.cm2MarkingWorkflow)
}


trait EditAssignmentDetailsValidation extends ModifyAssignmentDetailsValidation with ModifyMarkingWorkflowValidation {
	self: EditAssignmentDetailsCommandState with BooleanAssignmentDetailProperties with AssessmentServiceComponent with ModifyMarkingWorkflowState
		with UserLookupComponent =>

	override def validate(errors: Errors): Unit = {
		if (name != null && name.length < 3000) {
			val duplicates = assessmentService.getAssignmentByNameYearModule(name, academicYear, module).filter { existing => existing.isAlive && !(existing eq assignment) }
			for (duplicate <- duplicates.headOption) {
				errors.rejectValue("name", "name.duplicate.assignment", Array(name), "")
			}
		}
		genericValidate(errors)
	}
}


trait EditAssignmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditAssignmentDetailsCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		notDeleted(assignment)
		p.PermissionCheck(Permissions.Assignment.Update, module)
	}
}

trait EditAssignmentDetailsDescription extends Describable[Assignment] {

	self: EditAssignmentDetailsCommandState =>

	override lazy val eventName = "EditAssignmentDetails"

	override def describe(d: Description) {
		d.assignment(assignment).properties(
			"name" -> name,
			"openDate" -> openDate,
			"closeDate" -> closeDate,
			"workflowCtg" -> Option(workflowCategory).map(_.code).orNull,
			"workflowType" -> Option(workflowType).map(_.name).orNull
		)
	}

}
