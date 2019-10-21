package uk.ac.warwick.tabula.commands.cm2

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

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
  def applyInternal(): Feedback = feedback
}

trait ViewOnlineFeedbackCommandDescription extends Describable[Feedback] {
  self: ViewOnlineFeedbackCommandState =>

  override lazy val eventName = "ViewOnlineFeedback"

  def describe(d: Description): Unit =
    d.feedback(feedback)
}

trait ViewOnlineFeedbackCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ViewOnlineFeedbackCommandState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.AssignmentFeedback.Read, mandatory(feedback))
  }
}
