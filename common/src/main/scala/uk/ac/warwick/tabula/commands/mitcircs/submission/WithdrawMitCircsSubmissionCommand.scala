package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.submission.WithdrawMitCircsSubmissionCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmissionState.{OutcomesRecorded, ReadyForPanel, Submitted}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsSubmissionWithdrawnNotification
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object WithdrawMitCircsSubmissionCommand {
  type Result = CreateMitCircsSubmissionCommand.Result
  type Command =
    Appliable[Result]
      with WithdrawMitCircsSubmissionState
      with Notifies[Result, MitigatingCircumstancesSubmission]
      with SchedulesNotifications[Result, MitigatingCircumstancesSubmission]
      with CompletesNotifications[MitigatingCircumstancesSubmission]

  val RequiredPermission: Permission = CreateMitCircsSubmissionCommand.RequiredPermission

  def apply(submission: MitigatingCircumstancesSubmission, currentUser: User): Command =
    new WithdrawMitCircsSubmissionCommandInternal(submission, currentUser)
      with ComposableCommand[Result]
      with MitCircsSubmissionPermissions
      with WithdrawMitCircsSubmissionDescription
      with WithdrawMitCircsSubmissionNotifications
      with MitCircsSubmissionSchedulesNotifications // Cancel scheduling any reminders
      with MitCircsSubmissionNotificationCompletion // Complete any existing notifications
      with AutowiringMitCircsSubmissionServiceComponent
}

abstract class WithdrawMitCircsSubmissionCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[Result]
    with WithdrawMitCircsSubmissionState {
  self: MitCircsSubmissionServiceComponent =>

  override def applyInternal(): Result = transactional() {
    // Guarded at controller
    require(isSelf, "Only the student themselves can withdraw a mitigating circumstances submission")

    submission.withdraw()

    // Remove any existing share permissions
    val removeSharingCommand = MitCircsShareSubmissionCommand.remove(submission, currentUser)
    removeSharingCommand.usercodes.addAll(removeSharingCommand.grantedRole.toSeq.flatMap(_.users.knownType.allIncludedIds).asJava)
    removeSharingCommand.apply()

    submission.lastModified = DateTime.now
    submission.lastModifiedBy = currentUser
    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait WithdrawMitCircsSubmissionState extends EditMitCircsSubmissionState {
  val previousState: MitigatingCircumstancesSubmissionState = submission.state
}

trait WithdrawMitCircsSubmissionDescription extends Describable[Result] {
  self: WithdrawMitCircsSubmissionState =>

  override lazy val eventName: String = "WithdrawMitCircsSubmission"

  def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
}

trait WithdrawMitCircsSubmissionNotifications extends Notifies[Result, MitigatingCircumstancesSubmission] {
  self: WithdrawMitCircsSubmissionState =>

  override def emit(submission: Result): Seq[Notification[Result, MitigatingCircumstancesSubmission]] =
    // Only notify staff if the previous state was a submitted state
    if (Seq(Submitted, ReadyForPanel, OutcomesRecorded).contains(previousState))
      Seq(Notification.init(new MitCircsSubmissionWithdrawnNotification, currentUser, submission, submission))
    else
      Nil
}