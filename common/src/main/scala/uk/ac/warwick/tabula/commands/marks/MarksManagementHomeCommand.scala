package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.marks.MarksManagementHomeCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

object MarksManagementHomeCommand {
  case class MarksManagementHomeAdminInformation(
    moduleManagerDepartments: Seq[Department],
    adminDepartments: Seq[Department],
  )
  type Command = Appliable[MarksManagementHomeAdminInformation]

  val AdminPermission: Permission = Permissions.Feedback.Manage

  def apply(currentUser: CurrentUser): Command =
    new MarksManagementHomeCommandInternal(currentUser)
      with ComposableCommand[MarksManagementHomeAdminInformation]
      with PubliclyVisiblePermissions
      with Unaudited with ReadOnly
      with AutowiringModuleAndDepartmentServiceComponent
}

abstract class MarksManagementHomeCommandInternal(currentUser: CurrentUser)
  extends CommandInternal[MarksManagementHomeAdminInformation] {
  self: ModuleAndDepartmentServiceComponent =>

  override def applyInternal(): MarksManagementHomeAdminInformation = MarksManagementHomeAdminInformation(
    moduleManagerDepartments =
      moduleAndDepartmentService.modulesWithPermission(currentUser, AdminPermission)
        .map(_.adminDepartment).toSeq.sortBy(_.name),

    adminDepartments =
      moduleAndDepartmentService.departmentsWithPermission(currentUser, AdminPermission)
        .toSeq.sortBy(_.name),
  )
}
