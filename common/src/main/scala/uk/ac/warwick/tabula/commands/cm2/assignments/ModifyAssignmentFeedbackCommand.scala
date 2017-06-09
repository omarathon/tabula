package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object ModifyAssignmentFeedbackCommand {
	def apply(assignment: Assignment) =
		new ModifyAssignmentFeedbackCommandInternal(assignment)
			with ComposableCommand[Assignment]
			with ModifyAssignmentFeedbackPermissions
			with ModifyAssignmentFeedbackDescription
			with ModifyAssignmentFeedbackCommandState
			with AutowiringAssessmentServiceComponent
			with SharedAssignmentFeedbackProperties
			with AutowiringZipServiceComponent

}

class ModifyAssignmentFeedbackCommandInternal(override val assignment: Assignment)
	extends CommandInternal[Assignment] with PopulateOnForm {

	self: AssessmentServiceComponent with ModifyAssignmentFeedbackCommandState with SharedAssignmentFeedbackProperties =>

	override def applyInternal(): Assignment = {
		this.copyTo(assignment)
		assessmentService.save(assignment)
		assignment
	}

	override def populate(): Unit = {
		copySharedFeedbackFrom(assignment)
	}
}

trait ModifyAssignmentFeedbackCommandState {
	self: AssessmentServiceComponent with SharedAssignmentFeedbackProperties =>

	def assignment: Assignment

	def copyTo(assignment: Assignment) {
		copySharedFeedbackTo(assignment)
	}
}

trait ModifyAssignmentFeedbackDescription extends Describable[Assignment] {
	self: ModifyAssignmentFeedbackCommandState with SharedAssignmentFeedbackProperties =>

	override lazy val eventName: String = "ModifyAssignmentFeedback"

	override def describe(d: Description) {
		d.assignment(assignment).properties(
		if(feedbackTemplate != null) {
			"feedbackTemplate" -> feedbackTemplate.name
		}else {
			"feedbackTemplate" -> "No Template"
		},
		"automaticallyReleaseToMarkers" -> automaticallyReleaseToMarkers,
		"collectMarks" -> collectMarks,
		"summative" -> summative,
		"dissertation" -> dissertation
		)
	}
}

trait ModifyAssignmentFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyAssignmentFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		notDeleted(assignment)
		p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
	}
}