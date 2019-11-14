package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import MitCircsApproveOutcomesCommand._

object MitCircsApproveOutcomesCommand {

  type Result = MitigatingCircumstancesSubmission
  type Command = Appliable[Result] with MitCircsApproveOutcomesState
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(submission: MitigatingCircumstancesSubmission, currentUser: User) = new MitCircsApproveOutcomesCommandInternal(submission, currentUser)
    with ComposableCommand[MitigatingCircumstancesSubmission]
    with MitCircsApproveOutcomesPermissions
    with MitCircsApproveOutcomesDescription
    with MitCircsSubmissionSchedulesNotifications
    with AutowiringMitCircsSubmissionServiceComponent
}

class MitCircsApproveOutcomesCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[MitigatingCircumstancesSubmission] with MitCircsApproveOutcomesState {

  self: MitCircsSubmissionServiceComponent =>

  def applyInternal(): Result = transactional() {
    require(submission.panel.exists(_.chair == currentUser), "You must be the panel chair in order approve outcomes")
    submission.lastModifiedBy = currentUser
    submission.lastModified = DateTime.now
    if(approve) submission.approvedByChair(currentUser)
    else submission.unApprovedByChair()
    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait MitCircsApproveOutcomesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: MitCircsApproveOutcomesState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(RequiredPermission, submission)
  }
}

trait MitCircsApproveOutcomesDescription extends Describable[Result] {
  self: MitCircsApproveOutcomesState =>

  override lazy val eventName: String = if(approve) "MitCircsApproveOutcomes" else "MitCircsUnApproveOutcomes"

  def describe(d: Description): Unit = {
    d.mitigatingCircumstancesSubmission(submission)
  }
}

trait MitCircsApproveOutcomesState {
  val submission: MitigatingCircumstancesSubmission
  val currentUser: User
  var approve: Boolean = _
}
