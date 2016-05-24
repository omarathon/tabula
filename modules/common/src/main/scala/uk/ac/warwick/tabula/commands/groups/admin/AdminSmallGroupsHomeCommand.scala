package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroupSet, SmallGroupSetFilter}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel._
import uk.ac.warwick.tabula.services.groups.{AutowiringSmallGroupSetWorkflowServiceComponent, SmallGroupSetWorkflowServiceComponent}

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.permissions.Permissions
import AdminSmallGroupsHomeCommand._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._

case class AdminSmallGroupsHomeInformation(
	canAdminDepartment: Boolean,
	modulesWithPermission: Seq[Module],
	setsWithPermission: Seq[ViewSetMethods],
	departmentSmallGroupSets: Seq[DepartmentSmallGroupSet]
)

object AdminSmallGroupsHomeCommand {
	val RequiredPermission = Permissions.Module.ManageSmallGroups

	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser, calculateProgress: Boolean) =
		new AdminSmallGroupsHomeCommandInternal(department, academicYear, user, calculateProgress)
			with AdminSmallGroupsHomePermissionsRestrictedState
			with AdminSmallGroupsHomeCommandPermissions
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringSecurityServiceComponent
			with AutowiringSmallGroupServiceComponent
			with AutowiringSmallGroupSetWorkflowServiceComponent
			with ComposableCommand[AdminSmallGroupsHomeInformation]
			with ReadOnly with Unaudited
}

trait AdminSmallGroupsHomeCommandState {
	def department: Department
	def user: CurrentUser
	def academicYear: AcademicYear

	var moduleFilters: JList[SmallGroupSetFilter] = JArrayList()
	var statusFilters: JList[SmallGroupSetFilter] = JArrayList()
	var allocationMethodFilters: JList[SmallGroupSetFilter] = JArrayList()
	var termFilters: JList[SmallGroupSetFilter] = JArrayList()
}

trait AdminSmallGroupsHomePermissionsRestrictedState {
	self: AdminSmallGroupsHomeCommandState with ModuleAndDepartmentServiceComponent with SecurityServiceComponent =>

	lazy val canManageDepartment = securityService.can(user, RequiredPermission, department)
	lazy val modulesWithPermission = moduleAndDepartmentService.modulesWithPermission(user, RequiredPermission, department)
}

class AdminSmallGroupsHomeCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser, val calculateProgress: Boolean) extends CommandInternal[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState with TaskBenchmarking {
	self: AdminSmallGroupsHomePermissionsRestrictedState with SmallGroupServiceComponent with SecurityServiceComponent with SmallGroupSetWorkflowServiceComponent =>

	def applyInternal() = benchmarkTask("Build small groups admin home info") {
		val modules: Set[Module] =
			if (canManageDepartment) department.modules.asScala.toSet
			else modulesWithPermission

		val sets = benchmarkTask("Fetch, filter and sort sets") {
			smallGroupService.getSmallGroupSets(department, academicYear)
				.filter { set => moduleFilters.asScala.isEmpty || moduleFilters.asScala.exists { _(set) } }
				.filter { set => statusFilters.asScala.isEmpty || statusFilters.asScala.exists { _(set) } }
				.filter { set => allocationMethodFilters.asScala.isEmpty || allocationMethodFilters.asScala.exists { _(set) } }
				.filter { set => termFilters.asScala.isEmpty || termFilters.asScala.exists { _(set) } }
				.filter { set => canManageDepartment || modulesWithPermission.contains(set.module) || securityService.can(user, RequiredPermission, set) }
				.sortBy(set => (set.archived, set.module.code, set.nameWithoutModulePrefix))
		}

		val setViews: Seq[ViewSetMethods] =
			if (calculateProgress) benchmarkTask("Fetch progress information for sets") { sets.map { set =>
				if (set.archived) {
					ViewSet(
						set = set,
						groups = ViewGroup.fromGroups(set.groups.asScala.sorted),
						viewerRole = Tutor
					)
				} else {
					val progress = smallGroupSetWorkflowService.progress(set)

					ViewSetWithProgress(
						set = set,
						groups = ViewGroup.fromGroups(set.groups.asScala.sorted),
						viewerRole = Tutor,
						progress = SetProgress(progress.percentage, progress.cssClass, progress.messageCode),
						nextStage = progress.nextStage,
						stages = progress.stages
					)
				}
			}} else { sets.map { set =>
				ViewSet(
					set = set,
					groups = ViewGroup.fromGroups(set.groups.asScala.sorted),
					viewerRole = Tutor
				)
			}}

		AdminSmallGroupsHomeInformation(
			canAdminDepartment = canManageDepartment,
			modulesWithPermission = modules.toSeq.sorted,
			setsWithPermission = setViews,
			departmentSmallGroupSets = smallGroupService.getDepartmentSmallGroupSets(department, academicYear)
		)
	}
}

trait AdminSmallGroupsHomeCommandPermissions extends RequiresPermissionsChecking {
	self: AdminSmallGroupsHomeCommandState with SecurityServiceComponent with AdminSmallGroupsHomePermissionsRestrictedState =>

	def permissionsCheck(p: PermissionsChecking) {
		if (canManageDepartment) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			p.PermissionCheck(RequiredPermission, department)
		} else {
			val managedModules = modulesWithPermission.toList

			// This is implied by the above, but it's nice to check anyway. Avoid exception if there are no managed modules
			if (managedModules.nonEmpty) p.PermissionCheckAll(RequiredPermission, managedModules)
			else p.PermissionCheck(RequiredPermission, department)
		}
	}
}