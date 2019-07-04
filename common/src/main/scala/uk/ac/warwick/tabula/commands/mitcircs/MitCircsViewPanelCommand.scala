package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import MitCircsViewPanelCommand._
import uk.ac.warwick.tabula.WorkflowStages
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesPanel, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsWorkflowProgressServiceComponent, MitCircsWorkflowProgressServiceComponent}

import scala.collection.immutable.ListMap

case class MitCircsPanelInfo(
  panel: MitigatingCircumstancesPanel,
  submissionStages: Map[MitigatingCircumstancesSubmission, ListMap[String, WorkflowStages.StageProgress]]
)

object MitCircsViewPanelCommand {

  type Result = MitCircsPanelInfo
  type Command = Appliable[Result] with MitCircsViewPanelState
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(panel: MitigatingCircumstancesPanel) = new MitCircsViewPanelCommandInternal(panel)
    with ComposableCommand[Result]
    with MitCircsViewPanelPermissions
    with MitCircsViewPanelDescription
    with AutowiringMitCircsWorkflowProgressServiceComponent
}

class MitCircsViewPanelCommandInternal(val panel: MitigatingCircumstancesPanel) extends CommandInternal[Result] with MitCircsViewPanelState {

  self: MitCircsWorkflowProgressServiceComponent =>

  def applyInternal(): Result = transactional() {
    MitCircsPanelInfo(
      panel,
      panel.submissions.map{ s => s -> workflowProgressService.progress(s.department)(s).stages}.toMap
    )
  }
}

trait MitCircsViewPanelPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: MitCircsViewPanelState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(RequiredPermission,  mandatory(panel))
  }

}

trait MitCircsViewPanelDescription extends Describable[Result] {
  self: MitCircsViewPanelState =>

  def describe(d: Description) {
    d.mitigatingCircumstancesPanel(panel)
  }
}

trait MitCircsViewPanelState {
  val panel: MitigatingCircumstancesPanel
}