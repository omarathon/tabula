package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.Permissions._

case class MitigatingCircumstancesViewer(submission: MitigatingCircumstancesSubmission) extends BuiltInRole(MitigatingCircumstancesViewerRoleDefinition, submission)

case object MitigatingCircumstancesViewerRoleDefinition extends BuiltInRoleDefinition {
  override def description = "mitigating circumstances viewer"

  GrantsScopedPermission(
    MitigatingCircumstancesSubmission.Read,
  )

  override val canDelegateThisRolesPermissions: JavaImports.JBoolean = true
}
