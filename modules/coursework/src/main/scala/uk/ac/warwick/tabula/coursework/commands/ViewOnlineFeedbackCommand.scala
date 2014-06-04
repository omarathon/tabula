package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.commands.{Description, Describable, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions

object ViewOnlineFeedbackCommand {
	def apply(feedback: Feedback) =
		new ViewOnlineFeedbackCommandInternal(feedback)
			with ComposableCommand[Feedback]
			with ViewOnlineFeedbackCommandDescription
			with ViewOnlineFeedbackCommandPermissions
}

trait ViewOnlineFeedbackCommandState {
	def feedback: Feedback
}

class ViewOnlineFeedbackCommandInternal(val feedback: Feedback) extends CommandInternal[Feedback] with ViewOnlineFeedbackCommandState {
	def applyInternal() = feedback
}

trait ViewOnlineFeedbackCommandDescription extends Describable[Feedback] {
	self: ViewOnlineFeedbackCommandState =>

	override lazy val eventName = "ViewOnlineFeedback"

	def describe(d: Description) = d.assignment(feedback.assignment).properties("student" -> feedback.universityId)
}

trait ViewOnlineFeedbackCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewOnlineFeedbackCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.Read, mandatory(feedback))
	}
}