package uk.ac.warwick.tabula.web.controllers.admin.department

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.permissions.{GrantRoleCommand, RevokeRoleCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent, UserLookupService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.admin.AdminController

import scala.jdk.CollectionConverters._

trait DepartmentPermissionControllerMethods extends AdminController
  with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  @ModelAttribute("addCommand")
  def addCommandModel(@PathVariable department: Department): GrantRoleCommand.Command[Department] = GrantRoleCommand(department)

  @ModelAttribute("removeCommand")
  def removeCommandModel(@PathVariable department: Department): RevokeRoleCommand.Command[Department] = RevokeRoleCommand(department)

  // Should really be a RolesAndPermissions one, but they're not granted to any role, so just allow dept admins
  override val departmentPermission: Permission = Permissions.Department.ArrangeRoutesAndModules

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  var userLookup: UserLookupService = Wire[UserLookupService]

  def form(department: Department): Mav = {
    Mav("admin/department/permissions",
      "department" -> department
    ).crumbs(Breadcrumbs.Department(department))
  }

  def form(department: Department, usercodes: Seq[String], role: Option[RoleDefinition], action: String): Mav = {
    val users = userLookup.getUsersByUserIds(usercodes.asJava).asScala
    Mav("admin/department/permissions",
      "department" -> department,
      "users" -> users,
      "role" -> role,
      "action" -> action
    ).crumbs(Breadcrumbs.Department(department))
  }
}

@Controller
@RequestMapping(value = Array("/admin/department/{department}/permissions"))
class DepartmentPermissionController extends AdminController with DepartmentPermissionControllerMethods {

  @RequestMapping
  def permissionsForm(@PathVariable department: Department, @RequestParam(defaultValue = "") usercodes: Array[String],
    @RequestParam(value = "role", required = false) role: RoleDefinition, @RequestParam(value = "action", required = false) action: String): Mav =
    form(department, usercodes, Some(role), action)


}

@Controller
@RequestMapping(value = Array("/admin/department/{department}/permissions"))
class DepartmentAddPermissionController extends AdminController with DepartmentPermissionControllerMethods {

  validatesSelf[SelfValidating]

  @RequestMapping(method = Array(POST), params = Array("_command=add"))
  def addPermission(@Valid @ModelAttribute("addCommand") command: GrantRoleCommand.Command[Department], errors: Errors): Mav = {
    val department = command.scope
    if (errors.hasErrors) {
      form(department)
    } else {
      val role = Some(command.apply().roleDefinition)
      val userCodes = command.usercodes.asScala.toSeq
      form(department, userCodes, role, "add")
    }

  }
}

@Controller
@RequestMapping(value = Array("/admin/department/{department}/permissions"))
class DepartmentRemovePermissionController extends AdminController with DepartmentPermissionControllerMethods {

  validatesSelf[SelfValidating]

  @RequestMapping(method = Array(POST), params = Array("_command=remove"))
  def removePermission(@Valid @ModelAttribute("removeCommand") command: RevokeRoleCommand.Command[Department],
    errors: Errors): Mav = {
    val department = command.scope
    if (errors.hasErrors) {
      form(department)
    } else {
      val role = command.apply().map(_.roleDefinition)
      val userCodes = command.usercodes.asScala.toSeq
      form(department, userCodes, role, "remove")
    }

  }
}
