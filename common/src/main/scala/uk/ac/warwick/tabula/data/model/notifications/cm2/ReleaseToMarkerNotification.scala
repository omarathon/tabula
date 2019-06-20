package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, AutowiringUserLookupComponent}

import scala.collection.JavaConverters._

object ReleaseToMarkerNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/released_to_marker_notification.ftl"

  def renderCollectSubmissions(
    assignment: Assignment,
    allocatedStudentsCount: Int,
    studentsAtStagesCount: Seq[StudentAtStagesCount],
    feedbacksCount: Int,
    submissionsCount: Int,
    noSubmissionsWithExtensionCount: Int,
    noSubmissionsWithoutExtensionCount: Int,
    workflowVerb: String,
  ): FreemarkerModel = FreemarkerModel(templateLocation,
    Map(
      "assignment" -> assignment,
      "allocatedStudentsCount" -> allocatedStudentsCount,
      "studentsAtStagesCount" -> studentsAtStagesCount,
      "feedbacksCount" -> feedbacksCount,
      "submissionsCount" -> submissionsCount,
      "noSubmissionsWithExtensionCount" -> noSubmissionsWithExtensionCount,
      "noSubmissionsWithoutExtensionCount" -> noSubmissionsWithoutExtensionCount,
      "workflowVerb" -> workflowVerb
    )
  )

  def renderNoCollectingSubmissions(
    assignment: Assignment,
    numReleasedFeedbacks: Int,
    workflowVerb: String
  ): FreemarkerModel = {
    FreemarkerModel(templateLocation, Map(
      "assignment" -> assignment,
      "numReleasedFeedbacks" -> numReleasedFeedbacks,
      "workflowVerb" -> workflowVerb
    ))
  }
}

@Entity
@Proxy
@DiscriminatorValue("CM2ReleaseToMarker")
class ReleaseToMarkerNotification
  extends NotificationWithTarget[MarkerFeedback, Assignment]
    with SingleRecipientNotification
    with UserIdRecipientNotification
    with AutowiringUserLookupComponent
    with Logging
    with AutowiringCM2MarkingWorkflowServiceComponent
    with AllCompletedActionRequiredNotification {

  @transient
  lazy val helper: ReleaseToMarkerNotificationHelper = new ReleaseToMarkerNotificationHelper(assignment, recipient, cm2MarkingWorkflowService)

  def workflowVerb: String = items.asScala.headOption.map(_.entity.stage.verb).getOrElse(MarkingWorkflowStage.DefaultVerb)

  def verb = "released"

  def assignment: Assignment = target.entity

  def title: String = s"${assignment.module.code.toUpperCase}: ${assignment.name} has been released for marking"

  def content: FreemarkerModel = if (assignment.collectSubmissions) {
    ReleaseToMarkerNotification.renderCollectSubmissions(
      assignment = assignment,
      allocatedStudentsCount = helper.studentsAllocatedToThisMarker.size,
      studentsAtStagesCount = helper.studentsAtStagesCount,
      feedbacksCount = items.size,
      submissionsCount = helper.submissionsCount,
      noSubmissionsWithExtensionCount = helper.extensionsCount - helper.submissionsWithExtensionCount,
      noSubmissionsWithoutExtensionCount = {
        val noSumbmissionCount = helper.studentsAllocatedToThisMarker.size - helper.submissionsCount
        val noSubmissionWithExtension = helper.extensionsCount - helper.submissionsWithExtensionCount
        noSumbmissionCount - noSubmissionWithExtension
      },
      workflowVerb = workflowVerb
    )
  } else {
    ReleaseToMarkerNotification.renderNoCollectingSubmissions(
      assignment = assignment,
      numReleasedFeedbacks = items.size,
      workflowVerb = workflowVerb
    )
  }

  def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)

  def urlTitle = s"$workflowVerb the assignment '${assignment.module.code.toUpperCase} - ${assignment.name}'"

  priority = Warning

}

