package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import org.joda.time.{Days, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Info, Warning}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

trait FeedbackDueNotification extends AllCompletedActionRequiredNotification {
  self: Notification[_, Unit]
    with NotificationPreSaveBehaviour =>

  def deadline: Option[LocalDate]

  def assignment: Assignment

  def module: Module = assignment.module

  def moduleCode: String = module.code.toUpperCase

  @transient private lazy val workingDaysHelper = new WorkingDaysHelperImpl

  def calendarDaysLeft: Int = deadline.map { d =>
    val now = created.toLocalDate
    Days.daysBetween(now, d).getDays
  }.getOrElse(0)

  def daysLeft: Int =
    deadline.map { d =>
      val now = created.toLocalDate

      // need an offset, as the helper always includes both start and end date, off-by-one from what we want to show
      val offset =
        if (d.isBefore(now)) 1
        else -1 // today or in the future

      workingDaysHelper.getNumWorkingDays(now.asJava, d.asJava) + offset
    }.getOrElse(Integer.MAX_VALUE)

  def dueToday: Boolean = deadline.contains(created.toLocalDate)

  override final def onPreSave(newRecord: Boolean): Unit = {
    priority = if (daysLeft == 1) {
      Warning
    } else if (daysLeft < 1) {
      Critical
    } else {
      Info
    }
  }

  override final def verb = "publish"

  override final def urlTitle = "publish this feedback"

  override def url: String = Routes.admin.assignment.submissionsandfeedback(assignment)

}

@Entity
@Proxy
@DiscriminatorValue("FeedbackDueGeneral")
class FeedbackDueGeneralNotification
  extends BatchedNotification[Assignment, Unit, FeedbackDueGeneralNotification](FeedbackDueGeneralBatchedNotificationHandler)
    with SingleItemNotification[Assignment]
    with FeedbackDueNotification {

  override final def assignment: Assignment = item.entity

  override final def title: String = "%s: Feedback for \"%s\" is due to be published".format(assignment.module.code.toUpperCase, assignment.name)

  override final def recipients: Seq[User] = {
    if (deadline.nonEmpty && assignment.needsFeedbackPublishingIgnoreExtensions) {
      val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
      moduleAndDepartmentService.getModuleByCode(assignment.module.code)
        .getOrElse(throw new IllegalStateException("No such module"))
        .managers.users.toSeq
    } else {
      Seq()
    }
  }

  override final def deadline: Option[LocalDate] = assignment.feedbackDeadline

  override def content: FreemarkerModel = FreemarkerModel("/WEB-INF/freemarker/notifications/feedback_reminder_general.ftl", Map(
    "assignment" -> assignment,
    "daysLeft" -> daysLeft,
    "dateOnlyFormatter" -> dateOnlyFormatter,
    "deadline" -> deadline,
    "dueToday" -> dueToday
  ))
}

abstract class FeedbackDueBatchedNotificationHandler[A <: Notification[_, Unit] with FeedbackDueNotification] extends BatchedNotificationHandler[A] {
  protected def groupedByCalendarDaysLeft(batch: Seq[A]): Seq[(Int, Seq[A])] =
    batch.groupBy(_.calendarDaysLeft)
      .view
      .mapValues(_.sortBy(n => (n.calendarDaysLeft, n.deadline, n.moduleCode, n.assignment.name)))
      .toSeq
      .sortBy { case (calendarDaysLeft, _) => calendarDaysLeft }
}

object FeedbackDueGeneralBatchedNotificationHandler extends FeedbackDueBatchedNotificationHandler[FeedbackDueGeneralNotification] {
  override def titleForBatchInternal(notifications: Seq[FeedbackDueGeneralNotification], user: User): String = {
    val assignments = notifications.map(_.assignment).distinct

    if (assignments.size == 1) s"""${assignments.head.module.code.toUpperCase}: Feedback for "${assignments.head.name}" is due to be published"""
    else s"Feedback for ${assignments.size} assignments is due to be published"
  }

  private def daysLeftTimeStatementForBatch(calendarDaysLeft: Int, batchSize: Int): String = {
    val assignmentCount =
      if (batchSize > 1) s"$batchSize assignments"
      else "1 assignment"

    if (calendarDaysLeft > 1) {
      s"Feedback for $assignmentCount is due in $calendarDaysLeft days"
    } else if (calendarDaysLeft == 1) {
      s"Feedback for $assignmentCount is due tomorrow"
    } else if (calendarDaysLeft == 0) {
      s"Feedback for $assignmentCount is due today"
    } else if (calendarDaysLeft == -1) {
      s"Feedback for $assignmentCount is 1 day late"
    } else {
      s"Feedback for $assignmentCount is ${0 - calendarDaysLeft} days late"
    }
  }

  override def contentForBatchInternal(notifications: Seq[FeedbackDueGeneralNotification]): FreemarkerModel = {
    val batches = groupedByCalendarDaysLeft(notifications)

    FreemarkerModel("/WEB-INF/freemarker/notifications/feedback_reminder_general_batch.ftl", Map(
      "batches" -> batches.map { case (calendarDaysLeft, batch) =>
        Map(
          "timeStatement" -> daysLeftTimeStatementForBatch(calendarDaysLeft, batch.size),
          "items" -> batch.map(_.content.model)
        )
      }
    ))
  }

  override def urlForBatchInternal(notifications: Seq[FeedbackDueGeneralNotification], user: User): String =
    Routes.home

  override def urlTitleForBatchInternal(notifications: Seq[FeedbackDueGeneralNotification]): String =
    "view Coursework Management"
}

@Entity
@Proxy
@DiscriminatorValue("FeedbackDueExtension")
class FeedbackDueExtensionNotification
  extends BatchedNotification[Extension, Unit, FeedbackDueExtensionNotification](FeedbackDueExtensionBatchedNotificationHandler)
    with SingleItemNotification[Extension]
    with FeedbackDueNotification {

  final def extension: Extension = item.entity

  override final def assignment: Assignment = extension.assignment

  def submission: Option[Submission] = assignment.findSubmission(extension.usercode)

  override final def title: String = "%s: Feedback for %s for \"%s\" is due to be published".format(assignment.module.code.toUpperCase, extension.studentIdentifier, assignment.name)

  override final def recipients: Seq[User] = {
    // only send to recipients if the assignments needs feedback publishing and the student actually submitted
    if (submission.nonEmpty && deadline.nonEmpty && assignment.needsFeedbackPublishingFor(extension.usercode)) {
      val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
      moduleAndDepartmentService.getModuleByCode(assignment.module.code)
        .getOrElse(throw new IllegalStateException("No such module"))
        .managers.users.toSeq
    } else {
      Seq()
    }
  }

  override final def deadline: Option[LocalDate] = extension.feedbackDeadline.map(_.toLocalDate)

  override def content: FreemarkerModel = FreemarkerModel("/WEB-INF/freemarker/notifications/feedback_reminder_extension.ftl", Map(
    "extension" -> extension,
    "assignment" -> assignment,
    "daysLeft" -> daysLeft,
    "dateOnlyFormatter" -> dateOnlyFormatter,
    "deadline" -> deadline,
    "dueToday" -> dueToday
  ))
}

object FeedbackDueExtensionBatchedNotificationHandler extends FeedbackDueBatchedNotificationHandler[FeedbackDueExtensionNotification] {
  // Only batch notifications for the same assignment
  override def groupBatchInternal(notifications: Seq[FeedbackDueExtensionNotification]): Seq[Seq[FeedbackDueExtensionNotification]] =
    notifications.groupBy(_.assignment).values.toSeq

  override def titleForBatchInternal(notifications: Seq[FeedbackDueExtensionNotification], user: User): String = {
    val assignment = notifications.head.assignment

    s"""${assignment.module.code.toUpperCase}: Feedback for ${notifications.size} students for "${assignment.name}" is due to be published"""
  }

  private def daysLeftTimeStatementForBatch(calendarDaysLeft: Int, batchSize: Int): String = {
    val studentCount =
      if (batchSize > 1) s"$batchSize students"
      else "1 student"

    if (calendarDaysLeft > 1) {
      s"Feedback for $studentCount is due in $calendarDaysLeft days"
    } else if (calendarDaysLeft == 1) {
      s"Feedback for $studentCount is due tomorrow"
    } else if (calendarDaysLeft == 0) {
      s"Feedback for $studentCount is due today"
    } else if (calendarDaysLeft == -1) {
      s"Feedback for $studentCount is 1 day late"
    } else {
      s"Feedback for $studentCount is ${0 - calendarDaysLeft} days late"
    }
  }

  override def contentForBatchInternal(notifications: Seq[FeedbackDueExtensionNotification]): FreemarkerModel = {
    val batches = groupedByCalendarDaysLeft(notifications)

    FreemarkerModel("/WEB-INF/freemarker/notifications/feedback_reminder_extension_batch.ftl", Map(
      "assignment" -> notifications.head.assignment,
      "batches" -> batches.map { case (daysLeft, batch) =>
        Map(
          "timeStatement" -> daysLeftTimeStatementForBatch(daysLeft, batch.size),
          "items" -> batch.map(_.content.model)
        )
      }
    ))
  }

  override def urlForBatchInternal(notifications: Seq[FeedbackDueExtensionNotification], user: User): String =
    notifications.head.urlFor(user)

  override def urlTitleForBatchInternal(notifications: Seq[FeedbackDueExtensionNotification]): String =
    notifications.head.urlTitle
}
