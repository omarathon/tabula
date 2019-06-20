package uk.ac.warwick.tabula.commands.mitcircs

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.SendMessageCommand._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.{MitCircsAwaitingStudentMessageReminderNotification, MitCircsMessageFromStudentNotification, MitCircsMessageToStudentNotification, MitCircsMessageToStudentWithReminderNotification}
import uk.ac.warwick.tabula.data.model.{Notification, ScheduledNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object SendMessageCommand {
  type Result = MitigatingCircumstancesMessage
  type Command =
    Appliable[Result]
      with SendMessageState
      with SelfValidating
      with Notifies[Result, MitigatingCircumstancesSubmission]
      with SchedulesNotifications[Result, MitigatingCircumstancesSubmission]
      with CompletesNotifications[Result]
      with BindListener

  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Modify

  def apply(submission: MitigatingCircumstancesSubmission, currentUser: User): Command =
    new SendMessageCommandInternal(submission, currentUser)
      with ComposableCommand[Result]
      with SendMessageValidation
      with SendMessagePermissions
      with SendMessageDescription
      with SendMessageNotifications
      with SendMessageCompletesNotifications
      with SendMessageScheduledNotifications
      with AutowiringMitCircsSubmissionServiceComponent
}

abstract class SendMessageCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[Result]
    with SendMessageState
    with BindListener {
  self: MitCircsSubmissionServiceComponent =>

  override def onBind(result: BindingResult): Unit = transactional() {
    file.onBind(result)
  }

  override def applyInternal(): MitigatingCircumstancesMessage = transactional() {
    require(submission.canAddMessage, "Can't add a message to this submission")

    val newMessage = new MitigatingCircumstancesMessage(submission, currentUser)
    newMessage.message = message
    file.attached.asScala.foreach(newMessage.addAttachment)

    if (!studentSent)
      newMessage.replyByDate = replyByDate

    mitCircsSubmissionService.create(newMessage)
  }
}

trait SendMessageValidation extends SelfValidating {
  self: SendMessageState =>

  override def validate(errors: Errors): Unit = {
    if (!message.hasText)
      errors.rejectValue("message", "mitigatingCircumstances.message.required")

    if (!studentSent && replyByDate != null && replyByDate.isBefore(DateTime.now.plusDays(1)))
      errors.rejectValue("replyByDate", "mitigatingCircumstances.replyByDate.tooSoon")
  }
}

trait SendMessagePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: SendMessageState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(submission))
}

trait SendMessageDescription extends Describable[Result] {
  self: SendMessageState =>

  override lazy val eventName: String = "SendMessage"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
}

trait SendMessageState {
  val submission: MitigatingCircumstancesSubmission
  val currentUser: User

  /**
    * This should match the behaviour of MitigatingCircumstancesMessage.studentSent
    */
  lazy val studentSent: Boolean = currentUser == submission.student.asSsoUser

  var message: String = _

  @WithinYears(maxFuture = 0, maxPast = 0)
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  var replyByDate: DateTime = _

  var file: UploadedFile = new UploadedFile
}

/**
  * Notify the MCOs or student that a message has been sent
  */
trait SendMessageNotifications extends Notifies[MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission] {
  self: SendMessageState =>

  override def emit(message: MitigatingCircumstancesMessage): Seq[Notification[MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission]] = {
    if (studentSent) Seq(Notification.init(new MitCircsMessageFromStudentNotification, currentUser, message, submission))
    else if (replyByDate == null) Seq(Notification.init(new MitCircsMessageToStudentNotification, currentUser, message, submission))
    else Seq(Notification.init(new MitCircsMessageToStudentWithReminderNotification, currentUser, message, submission))
  }
}

/**
  * If the MCO has set a replyByDate, schedule a notification for the student to reply to the message
  */
trait SendMessageScheduledNotifications extends SchedulesNotifications[MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission] {
  self: SendMessageState with NotificationHandling =>

  override def transformResult(message: MitigatingCircumstancesMessage): Seq[MitigatingCircumstancesSubmission] =
    Seq(message.submission)

  override def scheduledNotifications(submission: MitigatingCircumstancesSubmission): Seq[ScheduledNotification[MitigatingCircumstancesSubmission]] =
    if (studentSent || replyByDate == null) Seq()
    else
      Seq(replyByDate.minusDays(1))
        .filter(_.isAfterNow)
        .map { when =>
          new ScheduledNotification[MitigatingCircumstancesSubmission]("MitCircsAwaitingStudentMessageReminder", submission, when)
        }
}

/**
  * Complete any notifications for the student where replyByDate is set
  */
trait SendMessageCompletesNotifications extends CompletesNotifications[MitigatingCircumstancesMessage] {
  self: SendMessageState with NotificationHandling =>

  override def notificationsToComplete(message: MitigatingCircumstancesMessage): CompletesNotificationsResult = {
    CompletesNotificationsResult(
      notificationService.findActionRequiredNotificationsByEntityAndType[MitCircsMessageToStudentWithReminderNotification](message.submission) ++
      notificationService.findActionRequiredNotificationsByEntityAndType[MitCircsAwaitingStudentMessageReminderNotification](message.submission),
      currentUser
    )
  }
}