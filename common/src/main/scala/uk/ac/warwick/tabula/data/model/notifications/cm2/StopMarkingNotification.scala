package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

object StopMarkingNotification {
  val templateLocation = "/WEB-INF/freemarker/emails/stop_marking_notification.ftl"
  val batchTemplateLocation = "/WEB-INF/freemarker/emails/stop_marking_notification_batch.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("CM2StopMarking")
class StopMarkingNotification
  extends BatchedNotificationWithTarget[MarkerFeedback, Assignment, StopMarkingNotification](StopMarkingBatchedNotificationHandler)
    with MyWarwickNotification
    with SingleRecipientNotification
    with UserIdRecipientNotification
    with AutowiringUserLookupComponent
    with Logging {

  def verb = "stopped"

  def assignment: Assignment = target.entity

  def title: String = "%s: Marking has been stopped for \"%s\"".format(assignment.module.code.toUpperCase, assignment.name)

  def content: FreemarkerModel = FreemarkerModel(StopMarkingNotification.templateLocation,
    Map(
      "assignment" -> assignment,
      "numStoppedFeedbacks" -> items.size
    ))

  def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)

  def urlTitle = s"view any outstanding marking"

  priority = Warning

}

object StopMarkingBatchedNotificationHandler extends BatchedNotificationHandler[StopMarkingNotification] {
  override def titleForBatchInternal(notifications: Seq[StopMarkingNotification], user: User): String ={
    val assignments = notifications.map(_.assignment).distinct

    if (assignments.size == 1) notifications.head.titleFor(user)
    else s"Marking has been stopped for ${assignments.size} assignments"
  }

  override def contentForBatchInternal(notifications: Seq[StopMarkingNotification]): FreemarkerModel = {
    // Get assignments and submission count
    val assignments: Seq[(Assignment, Int)] =
      notifications.groupBy(_.assignment).toSeq
        .map { case (assignment, batch) =>
          assignment -> batch.map(_.items.size).sum
        }

    FreemarkerModel(StopMarkingNotification.batchTemplateLocation, Map(
      "assignments" -> assignments,
      "totalNumStoppedFeedbacks" -> assignments.map(_._2).sum,
    ))
  }

  override def urlForBatchInternal(notifications: Seq[StopMarkingNotification], user: User): String =
    Routes.marker()

  override def urlTitleForBatchInternal(notifications: Seq[StopMarkingNotification]): String =
    "view assignments for marking"
}
