package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ModifyAssignmentSubmissionsCommand {
	def apply(assignment: Assignment) =
		new ModifyAssignmentSubmissionsCommandInternal(assignment)
			with ComposableCommand[Assignment]
			with ModifyAssignmentSubmissionsPermissions
			with ModifyAssignmentSubmissionsDescription
			with ModifyAssignmentSubmissionsValidation
			with ModifyAssignmentSubmissionsCommandState
			with ModifyAssignmentScheduledNotifications
			with AutowiringAssessmentServiceComponent
			with SharedAssignmentSubmissionProperties
}

class ModifyAssignmentSubmissionsCommandInternal(override val assignment: Assignment)
	extends CommandInternal[Assignment] with PopulateOnForm {

	self: AssessmentServiceComponent with ModifyAssignmentSubmissionsCommandState with SharedAssignmentSubmissionProperties =>


	override def applyInternal(): Assignment = {
		this.copyTo(assignment)
		assessmentService.save(assignment)
		assignment
	}

	override def populate(): Unit = {
		copySharedSubmissionFrom(assignment)
	}

}

trait ModifyAssignmentSubmissionsCommandState {
	self: AssessmentServiceComponent with SharedAssignmentSubmissionProperties =>

	def assignment: Assignment

	def copyTo(assignment: Assignment) {
		copySharedSubmissionTo(assignment)
	}

}


trait ModifyAssignmentSubmissionsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyAssignmentSubmissionsCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		notDeleted(assignment)
		p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
	}
}


trait ModifyAssignmentSubmissionsDescription extends Describable[Assignment] {
	self: ModifyAssignmentSubmissionsCommandState with SharedAssignmentSubmissionProperties =>

	override lazy val eventName: String = "ModifyAssignmentSubmissions"

	override def describe(d: Description) {
		d.assignment(assignment)
		d.properties(
			"collectSubmissions" -> collectSubmissions,
			"automaticallySubmitToTurnitin" -> automaticallySubmitToTurnitin,
			"displayPlagiarismNotice" -> displayPlagiarismNotice,
			"restrictSubmissions" -> restrictSubmissions,
			"allowResubmission" -> allowResubmission,
			"allowLateSubmissions" -> allowLateSubmissions,
			"allowExtensions" -> allowExtensions,
			"extensionAttachmentMandatory" -> extensionAttachmentMandatory,
			"allowExtensionsAfterCloseDate" -> allowExtensionsAfterCloseDate
		)
	}
}

trait ModifyAssignmentSubmissionsValidation extends SelfValidating {
	self: ModifyAssignmentSubmissionsCommandState with SharedAssignmentSubmissionProperties =>

	override def validate(errors: Errors): Unit = {
		if (!restrictSubmissions && assignment.hasCM2Workflow) {
			errors.rejectValue("restrictSubmissions", "assignment.restrictSubmissions.hasWorkflow")
		}
	}
}
