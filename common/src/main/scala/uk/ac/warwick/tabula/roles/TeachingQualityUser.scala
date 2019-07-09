package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._

case class TeachingQualityUser() extends BuiltInRole(TeachingQualityRoleDefinition, None)

case object TeachingQualityRoleDefinition extends UnassignableBuiltInRoleDefinition {

  override def description = "Teaching quality user"

  GrantsGlobalPermission(
    MitigatingCircumstancesSubmission.Read
  )

}