package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.permissions.{GrantRoleCommand, RevokeRoleCommand}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, RoleDefinition}
import uk.ac.warwick.tabula.web.Mav

import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/sysadmin/departments"))
class SysadminDeptDetailsController extends BaseSysadminController {

  @RequestMapping
  def departments =
    Mav("sysadmin/departments/list", "departments" -> moduleService.allDepartments.sortBy(_.name))

  @RequestMapping(Array("/{dept}/"))
  def department(@PathVariable dept: Department): Mav = {
    mandatory(dept)
    Mav("sysadmin/departments/single", "department" -> dept).crumbs(SysadminBreadcrumbs.Departments.Home)
  }
}

trait DepartmentPermissionControllerMethods extends BaseSysadminController {

  @ModelAttribute("addCommand") def addCommandModel(@PathVariable department: Department): GrantRoleCommand.Command[Department] =
    GrantRoleCommand(mandatory(department))

  @ModelAttribute("removeCommand") def removeCommandModel(@PathVariable department: Department): RevokeRoleCommand.Command[Department] =
    RevokeRoleCommand(mandatory(department))

  def form(@PathVariable department: Department): Mav = {
    Mav("sysadmin/departments/permissions", "department" -> department).crumbs(
      SysadminBreadcrumbs.Departments.Home,
      SysadminBreadcrumbs.Departments.Department(department)
    )
  }

  def form(department: Department, usercodes: Seq[String], role: Option[RoleDefinition], action: String): Mav = {
    val users = userLookup.getUsersByUserIds(usercodes.asJava)
    Mav("sysadmin/departments/permissions",
      "department" -> department,
      "users" -> users,
      "role" -> role,
      "action" -> action
    ).crumbs(
      SysadminBreadcrumbs.Departments.Home,
      SysadminBreadcrumbs.Departments.Department(department)
    )
  }
}

@Controller
@RequestMapping(Array("/sysadmin/departments/{department}/permissions"))
class SysadminDepartmentPermissionController extends BaseSysadminController with DepartmentPermissionControllerMethods {
  @RequestMapping
  def permissionsForm(@PathVariable department: Department): Mav =
    form(mandatory(department))
}

@Controller
@RequestMapping(Array("/sysadmin/departments/{department}/permissions"))
class SysadminDepartmentAddPermissionController extends BaseSysadminController with DepartmentPermissionControllerMethods {

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
@RequestMapping(Array("/sysadmin/departments/{department}/permissions"))
class SysadminDepartmentRemovePermissionController extends BaseSysadminController with DepartmentPermissionControllerMethods {

  validatesSelf[SelfValidating]

  @RequestMapping(method = Array(POST), params = Array("_command=remove"))
  def addPermission(@Valid @ModelAttribute("removeCommand") command: RevokeRoleCommand.Command[Department], errors: Errors): Mav = {
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
