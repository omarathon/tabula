package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesStudent

case class MitigatingCircumstancesSubmitter(student: MitigatingCircumstancesStudent) extends BuiltInRole(MitigatingCircumstancesSubmitterRoleDefinition, student)

case object MitigatingCircumstancesSubmitterRoleDefinition extends UnassignableBuiltInRoleDefinition {
  override def description = "In a department that manages mitigating circumstances in Tabula"

  GeneratesSubRole(MitigatingCircumstancesViewerRoleDefinition)

  GrantsScopedPermission(
    MitigatingCircumstancesSubmission.Read,
    MitigatingCircumstancesSubmission.Modify,
    MitigatingCircumstancesSubmission.Share,
  )
}
