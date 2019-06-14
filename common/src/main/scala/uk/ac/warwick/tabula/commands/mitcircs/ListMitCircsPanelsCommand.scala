package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.mitcircs.ListMitCircsPanelsCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsPanelServiceComponent, MitCircsPanelServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ListMitCircsPanelsCommand {
  type Result = Seq[MitigatingCircumstancesPanel]
  type Command = Appliable[Result] with ListMitCircsPanelsState
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(department: Department, year: AcademicYear): Command =
    new ListMitCircsPanelsCommandInternal(department, year)
      with ComposableCommand[Result]
      with AutowiringMitCircsPanelServiceComponent
      with ListMitCircsPanelsPermissions
      with ReadOnly with Unaudited
}

abstract class ListMitCircsPanelsCommandInternal(val department: Department, val year: AcademicYear)
  extends CommandInternal[Result]
    with ListMitCircsPanelsState {
  self: MitCircsPanelServiceComponent =>

  override def applyInternal(): Result =
    mitCircsPanelService.list(department, year)
}

trait ListMitCircsPanelsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListMitCircsPanelsState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(department))
}

trait ListMitCircsPanelsState {
  // This is used to filter the department the panel is assigned to
  def department: Department

  // This is used to filter the panel dates
  def year: AcademicYear
}
