package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent

trait DepartmentsAndModulesWithPermission {

  self: ModuleAndDepartmentServiceComponent =>

  case class Result(departments: Set[Department], modules: Set[Module])

  def departmentsAndModulesForPermission(user: CurrentUser, permission: Permission): Result = {
    val departments = moduleAndDepartmentService.departmentsWithPermission(user, permission)
    val modules = moduleAndDepartmentService.modulesWithPermission(user, permission)
    Result(departments, modules)
  }

  def allDepartmentsForPermission(user: CurrentUser, permission: Permission): Set[Department] = {
    val result = departmentsAndModulesForPermission(user, permission)
    result.departments ++ result.modules.map(_.adminDepartment)
  }
}