package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

// TODO - pointless as is but I imagine that we may want to split submissions into a few lists later so plumbing this in now
case class AdminHomeInformation(
  submissions: Seq[MitigatingCircumstancesSubmission]
)

object AdminHomeCommand {
  def apply(department: Department) = new AdminHomeCommandInternal(department)
    with ComposableCommand[AdminHomeInformation]
    with AutowiringMitCircsSubmissionServiceComponent
    with AdminHomePermissions
    with ReadOnly with Unaudited
}

abstract class AdminHomeCommandInternal(val department: Department) extends CommandInternal[AdminHomeInformation] with AdminHomeCommandState {
  self: MitCircsSubmissionServiceComponent =>

  override def applyInternal(): AdminHomeInformation = {
    val submissions = mitCircsSubmissionService.submissionsForDepartment(department)
    AdminHomeInformation(submissions)
  }
}

trait AdminHomePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AdminHomeCommandState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.MitigatingCircumstancesSubmission.Manage, department)
  }
}

trait AdminHomeCommandState {
  def department: Department
}
