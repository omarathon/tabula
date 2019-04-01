package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.JavaImports

case class MitigatingCircumstancesSubmitter(student: model.StudentMember) extends BuiltInRole(MitigatingCircumstancesSubmitterRoleDefinition, student)

case object MitigatingCircumstancesSubmitterRoleDefinition extends UnassignableBuiltInRoleDefinition {
  override def description = "In a department that manages mitigating circumstances in Tabula"

  GrantsScopedPermission(
    MitigatingCircumstancesSubmission.Modify
  )
}