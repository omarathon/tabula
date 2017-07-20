package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

object GenericFeedbackCommand {
	def apply(assignment: Assignment) =
		new GenericFeedbackCommand(assignment)
			with ComposableCommand[Assignment]
			with AutowiringAssessmentServiceComponent
			with GenericFeedbackPermissions
			with GenericFeedbackDescription[Assignment] {
			override lazy val eventName = "GenericFeedback"
		}
}

abstract class GenericFeedbackCommand(val assignment: Assignment)
	extends CommandInternal[Assignment] with Appliable[Assignment] with GenericFeedbackState {
	self: AssessmentServiceComponent =>

	genericFeedback = assignment.genericFeedback

	def applyInternal(): Assignment = {
		assignment.genericFeedback = genericFeedback
		assessmentService.save(assignment)
		assignment
	}
}

trait GenericFeedbackPermissions extends RequiresPermissionsChecking {
	self: GenericFeedbackState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
	}
}

trait GenericFeedbackState {
	val assignment: Assignment
	var genericFeedback: String = _
}

trait GenericFeedbackDescription[A] extends Describable[A] {
	self: GenericFeedbackState =>

	def describe(d: Description) {
		d.assignment(assignment)
	}
}