package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object ModifyFeedbackCommand {
  def apply(assignment: Assignment) =
    new ModifyFeedbackCommandInternal(assignment)
      with ComposableCommand[Assignment]
      with ModifyFeedbackPermissions
      with ModifyFeedbackDescription
      with ModifyFeedbackCommandState
      with ModifyAssignmentScheduledNotifications
      with AutowiringAssessmentServiceComponent
      with SharedFeedbackProperties
      with AutowiringZipServiceComponent

}

class ModifyFeedbackCommandInternal(override val assignment: Assignment)
  extends CommandInternal[Assignment] with PopulateOnForm {

  self: AssessmentServiceComponent with ModifyFeedbackCommandState with SharedFeedbackProperties =>

  override def applyInternal(): Assignment = {
    this.copyTo(assignment)
    assessmentService.save(assignment)
    assignment
  }

  override def populate(): Unit = {
    copySharedFeedbackFrom(assignment)
  }
}

trait ModifyFeedbackCommandState {
  self: AssessmentServiceComponent with SharedFeedbackProperties =>

  def assignment: Assignment

  def copyTo(assignment: Assignment): Unit = {
    copySharedFeedbackTo(assignment)
  }
}

trait ModifyFeedbackRequest extends SharedFeedbackProperties {
  self: ZipServiceComponent =>
}

trait ModifyFeedbackDescription extends Describable[Assignment] {
  self: ModifyFeedbackCommandState with SharedFeedbackProperties =>

  override lazy val eventName: String = "ModifyAssignmentFeedback"

  override def describe(d: Description): Unit = {
    d.assignment(assignment).properties(
      if (feedbackTemplate != null) {
        "feedbackTemplate" -> feedbackTemplate.name
      } else {
        "feedbackTemplate" -> "-"
      },
      "automaticallyReleaseToMarkers" -> automaticallyReleaseToMarkers,
      "collectMarks" -> collectMarks,
      "useMarkPoints" -> useMarkPoints,
      "summative" -> summative,
      "dissertation" -> dissertation
    )
  }
}

trait ModifyFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ModifyFeedbackCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    notDeleted(assignment)
    p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
  }
}
