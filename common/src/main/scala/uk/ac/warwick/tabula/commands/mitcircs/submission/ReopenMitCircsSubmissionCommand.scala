package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.submission.ReopenMitCircsSubmissionCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.userlookup.User

object ReopenMitCircsSubmissionCommand {
  type Result = CreateMitCircsSubmissionCommand.Result
  type Command =
    Appliable[Result]
      with EditMitCircsSubmissionState
      with SchedulesNotifications[Result, MitigatingCircumstancesSubmission]

  val RequiredPermission: Permission = CreateMitCircsSubmissionCommand.RequiredPermission

  def apply(submission: MitigatingCircumstancesSubmission, currentUser: User): Command =
    new ReopenMitCircsSubmissionCommandInternal(submission, currentUser)
      with ComposableCommand[Result]
      with MitCircsSubmissionPermissions
      with ReopenMitCircsSubmissionDescription
      with MitCircsSubmissionSchedulesNotifications // Schedule draft reminders
      with AutowiringMitCircsSubmissionServiceComponent
}

abstract class ReopenMitCircsSubmissionCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[Result]
    with EditMitCircsSubmissionState {
  self: MitCircsSubmissionServiceComponent =>

  override def applyInternal(): Result = transactional() {
    // Guarded at controller
    require(isSelf, "Only the student themselves can Reopen a mitigating circumstances submission")

    submission.reopen()
    submission.lastModified = DateTime.now
    submission.lastModifiedBy = currentUser
    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait ReopenMitCircsSubmissionDescription extends Describable[Result] {
  self: EditMitCircsSubmissionState =>

  override lazy val eventName: String = "ReopenMitCircsSubmission"

  def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
}
