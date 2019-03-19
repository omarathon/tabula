package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.JavaImports._

case class MitigatingCircumstancesOfficer(department: model.Department) extends BuiltInRole(MitigatingCircumstancesOfficerRoleDefinition, department)

case object MitigatingCircumstancesOfficerRoleDefinition extends BuiltInRoleDefinition {

  override def description = "Mitigating Circumstances Officer"

  GrantsScopedPermission(
    MitigatingCircumstancesSubmission.Manage,
    MitigatingCircumstancesSubmission.Read,
    MitigatingCircumstancesSubmission.ViewGrading
  )

  def canDelegateThisRolesPermissions: JBoolean = true

}
