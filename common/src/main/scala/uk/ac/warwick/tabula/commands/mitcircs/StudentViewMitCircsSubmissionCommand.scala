package uk.ac.warwick.tabula.commands.mitcircs

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.StudentViewMitCircsSubmissionCommand._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object StudentViewMitCircsSubmissionCommand {
  type Result = MitigatingCircumstancesSubmission
  type Command = Appliable[Result]
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(submission: MitigatingCircumstancesSubmission, user: User): Command =
    new StudentViewMitCircsSubmissionCommandInternal(submission, user)
      with ComposableCommand[Result]
      with StudentViewMitCircsSubmissionPermissions
      with StudentViewMitCircsSubmissionDescription
      with AutowiringMitCircsSubmissionServiceComponent
}

abstract class StudentViewMitCircsSubmissionCommandInternal(val submission: MitigatingCircumstancesSubmission, val user: User)
  extends CommandInternal[Result]
    with StudentViewMitCircsSubmissionState {
  self: MitCircsSubmissionServiceComponent =>

  override def applyInternal(): MitigatingCircumstancesSubmission = transactional() {
    if (user.getWarwickId == submission.student.universityId) {
      submission.lastViewedByStudent = DateTime.now
      mitCircsSubmissionService.saveOrUpdate(submission)
    }

    // TODO find out why this is necessary for related submissions which themselves are linked to a related submission
    Option(submission.relatedSubmission).foreach(HibernateHelpers.initialiseAndUnproxy)
    HibernateHelpers.initialiseAndUnproxy(submission)
  }
}

trait StudentViewMitCircsSubmissionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentViewMitCircsSubmissionState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(submission))
}

trait StudentViewMitCircsSubmissionDescription extends Describable[Result] {
  self: StudentViewMitCircsSubmissionState =>

  override lazy val eventName: String = "StudentViewMitCircsSubmission"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
}

trait StudentViewMitCircsSubmissionState {
  def submission: MitigatingCircumstancesSubmission
  def user: User
}
