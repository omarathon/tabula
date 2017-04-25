package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.ListAssignmentsCommand._
import uk.ac.warwick.tabula.data.model.{Department, MarkingMethod, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringSecurityServiceComponent, ModuleAndDepartmentServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object ListAssignmentsCommand {
	case class ListAssignmentsInfo(
		module: Module
	)

	type Result = Seq[ListAssignmentsInfo]
	type Command = Appliable[Result] with ListAssignmentsCommandState

	val AdminPermission = Permissions.Module.ManageAssignments

	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser): Command =
		new ListAssignmentsCommandInternal(department, academicYear, user)
			with ListAssignmentsPermissions
			with ListAssignmentsModulesWithPermission
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringSecurityServiceComponent
			with ComposableCommand[Result]
			with Unaudited with ReadOnly
}

trait ListAssignmentsCommandState {
	def department: Department
	def academicYear: AcademicYear
	def user: CurrentUser
}

trait ListAssignmentsCommandRequest {
	self: ListAssignmentsCommandState =>

	var filterModules: JSet[Module] = JHashSet()
	var filterWorkflowTypes: JSet[MarkingMethod] = JHashSet()
	var filterAssignmentStatuses: JSet[Object] = JHashSet() // TODO
	var filterAssignmentActions: JSet[Object] = JHashSet() // TODO
}

class ListAssignmentsCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
	extends CommandInternal[Result] with ListAssignmentsCommandState with ListAssignmentsCommandRequest {
	self: ListAssignmentsModulesWithPermission
		with SecurityServiceComponent =>

	lazy val modules: Seq[Module] =
		if (securityService.can(user, AdminPermission, department)) {
			department.modules.asScala
		} else {
			modulesWithPermission.toList.sorted
		}

	override def applyInternal(): Result = modules.map { module =>
		ListAssignmentsInfo(module)
	}

}

trait ListAssignmentsModulesWithPermission {
	self: ListAssignmentsCommandState
		with ModuleAndDepartmentServiceComponent =>

	lazy val modulesWithPermission: Set[Module] =
		moduleAndDepartmentService.modulesWithPermission(user, AdminPermission, department)
}

trait ListAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListAssignmentsCommandState
		with ListAssignmentsModulesWithPermission
		with SecurityServiceComponent =>

	override def permissionsCheck(p: PermissionsChecking): Unit =
		if (securityService.can(user, AdminPermission, mandatory(department))) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			p.PermissionCheck(AdminPermission, department)
		} else {
			val managedModules = modulesWithPermission.toList

			// This is implied by the above, but it's nice to check anyway. Avoid exception if there are no managed modules
			if (managedModules.nonEmpty) p.PermissionCheckAll(AdminPermission, managedModules)
			else p.PermissionCheck(AdminPermission, department)
		}
}