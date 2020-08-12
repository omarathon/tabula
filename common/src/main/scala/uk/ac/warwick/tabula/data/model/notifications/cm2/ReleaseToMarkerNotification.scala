package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, CM2MarkingWorkflowService}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object ReleaseToMarkerNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/released_to_marker_notification.ftl"
  val batchTemplateLocation: String = "/WEB-INF/freemarker/emails/released_to_marker_notification_batch.ftl"

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
      "feedbackDeadlineDate" -> assignment.feedbackDeadline.map(DateFormats.NotificationDateOnly.print),
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
    feedbacksCount: Int,
    workflowVerb: String
  ): FreemarkerModel = {
    FreemarkerModel(templateLocation, Map(
      "assignment" -> assignment,
      "feedbacksCount" -> feedbacksCount,
      "workflowVerb" -> workflowVerb
    ))
  }
}

@Entity
@Proxy
@DiscriminatorValue("CM2ReleaseToMarker")
class ReleaseToMarkerNotification
  extends BatchedNotificationWithTarget[MarkerFeedback, Assignment, ReleaseToMarkerNotification](ReleaseToMarkerBatchedNotificationHandler)
    with SingleRecipientNotification
    with UserIdRecipientNotification
    with AutowiringUserLookupComponent
    with Logging
    with AllCompletedActionRequiredNotification {

  @transient
  var cm2MarkingWorkflowService: CM2MarkingWorkflowService = Wire.auto[CM2MarkingWorkflowService]

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
      feedbacksCount = items.size,
      workflowVerb = workflowVerb
    )
  }

  def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)

  def urlTitle = s"$workflowVerb the assignment '${assignment.module.code.toUpperCase} - ${assignment.name}'"

  priority = Warning

}

object ReleaseToMarkerBatchedNotificationHandler extends BatchedNotificationHandler[ReleaseToMarkerNotification] {
  override def titleForBatchInternal(notifications: Seq[ReleaseToMarkerNotification], user: User): String = {
    val assignments = notifications.map(_.assignment).distinct

    if (assignments.size == 1) notifications.head.titleFor(user)
    else s"${assignments.size} assignments have been released for marking"
  }

  override def contentForBatchInternal(notifications: Seq[ReleaseToMarkerNotification]): FreemarkerModel = {
    // We only retain the last notification for each assignment
    val assignments =
      notifications.groupBy(_.assignment).toSeq
        .map { case (assignment, batch) =>
          assignment -> batch.maxBy(_.created)
        }
        .sortBy(_._2.created)

    if (assignments.size == 1) assignments.head._2.content
    else FreemarkerModel(ReleaseToMarkerNotification.batchTemplateLocation, Map(
      "notifications" -> assignments.map(_._2.content.model)
    ))
  }

  override def urlForBatchInternal(notifications: Seq[ReleaseToMarkerNotification], user: User): String =
    Routes.marker()

  override def urlTitleForBatchInternal(notifications: Seq[ReleaseToMarkerNotification]): String =
    "view assignments for marking"
}
