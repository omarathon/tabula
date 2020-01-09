package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._


object ReleaseForMarkingCommand {
  def apply(assignment: Assignment, user: User) = new ReleaseForMarkingCommandInternal(assignment, user)
    with ComposableCommand[Seq[Feedback]]
    with ReleaseForMarkingValidation
    with ReleaseForMarkingPermissions
    with ReleaseForMarkingDescription
    with FeedbackReleasedNotifier
    with AutowiringCM2MarkingWorkflowServiceComponent
}

class ReleaseForMarkingCommandInternal(val assignment: Assignment, val user: User)
  extends CommandInternal[Seq[Feedback]] with ReleaseForMarkingState with ReleaseForMarkingRequest with ReleasedState {

  self: CM2MarkingWorkflowServiceComponent =>

  def applyInternal(): Seq[Feedback] = {
    val feedbackForRelease = feedbacks.filterNot(f => unreleasableSubmissions.contains(f.usercode))
    val feedback = cm2MarkingWorkflowService.releaseForMarking(feedbackForRelease)
    newReleasedFeedback = feedback.flatMap(_.markingInProgress).asJava
    feedback
  }
}

trait ReleaseForMarkingPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ReleaseForMarkingState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Submission.ReleaseForMarking, assignment)
  }
}

trait ReleaseForMarkingValidation extends SelfValidating {
  self: ReleaseForMarkingRequest =>
  def validate(errors: Errors): Unit = {
    if (!confirm) errors.rejectValue("confirm", "submission.release.for.marking.confirm")
  }
}

trait ReleaseForMarkingDescription extends Describable[Seq[Feedback]] {
  self: ReleaseForMarkingState with ReleaseForMarkingRequest =>

  override lazy val eventName: String = "ReleaseForMarking"

  override def describe(d: Description): Unit =
    d.assignment(assignment)
     .studentUsercodes(students.asScala.toSeq)

  override def describeResult(d: Description, result: Seq[Feedback]): Unit =
    d.assignment(assignment)
     .feedbacks(result)
}

trait ReleaseForMarkingState extends SelectedStudentsState with UserAware {
  def assignment: Assignment
}

trait ReleaseForMarkingRequest extends SelectedStudentsRequest {
  self: ReleaseForMarkingState =>
  var confirm: Boolean = false

  def studentsWithoutKnownMarkers: Seq[String] = {
    val neverAssigned = students.asScala.toSeq diff feedbacks.filter(f => {
      val stagesWithoutAllocation = assignment.cm2MarkingWorkflow.allStages.filter(_.hasMarkers).toSet -- f.allMarkerFeedback.map(_.stage)
      stagesWithoutAllocation.isEmpty
    }).map(_.usercode)

    val markerRemoved = feedbacks.filter(f => {
      val initialStageFeedback = f.allMarkerFeedback.filter(mf => assignment.cm2MarkingWorkflow.initialStages.contains(mf.stage))
      initialStageFeedback.nonEmpty && !initialStageFeedback.exists(_.marker.isFoundUser)
    }).map(_.usercode)

    neverAssigned ++ markerRemoved
  }

  def studentsAlreadyReleased: Seq[String] = feedbacks.filter(_.outstandingStages.asScala.nonEmpty).map(_.usercode)

  def unreleasableSubmissions: Seq[String] = studentsWithoutKnownMarkers ++ studentsAlreadyReleased
}
