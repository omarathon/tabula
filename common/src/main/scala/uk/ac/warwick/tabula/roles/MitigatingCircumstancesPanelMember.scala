package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel

case class MitigatingCircumstancesPanelMember(panel: MitigatingCircumstancesPanel) extends BuiltInRole(MitigatingCircumstancesPanelMemberRoleDefinition, panel)

case object MitigatingCircumstancesPanelMemberRoleDefinition extends UnassignableBuiltInRoleDefinition {
  override def description = "member of a mitigating circumstances panel"

  GeneratesSubRole(MitigatingCircumstancesViewerRoleDefinition)

  GrantsScopedPermission(
    Profiles.Read.ReasonableAdjustments,
    Profiles.Read.ReasonableAdjustmentsNotes,
  )
}