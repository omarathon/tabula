package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.services.{SmallGroupSetWorkflowServiceComponent, AutowiringSmallGroupSetWorkflowServiceComponent}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel._

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.permissions.Permissions
import AdminSmallGroupsHomeCommand._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

case class AdminSmallGroupsHomeInformation(
	canAdminDepartment: Boolean,
	modulesWithPermission: Seq[Module],
	setsWithPermission: Seq[ViewSetWithProgress]
)

object AdminSmallGroupsHomeCommand {
	val RequiredPermission = Permissions.Module.ManageSmallGroups

	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new AdminSmallGroupsHomeCommandInternal(department, academicYear, user)
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
}

trait AdminSmallGroupsHomePermissionsRestrictedState {
	self: AdminSmallGroupsHomeCommandState with ModuleAndDepartmentServiceComponent with SecurityServiceComponent =>

	lazy val canManageDepartment = securityService.can(user, RequiredPermission, department)
	lazy val modulesWithPermission = moduleAndDepartmentService.modulesWithPermission(user, RequiredPermission, department)
}

class AdminSmallGroupsHomeCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser) extends CommandInternal[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState {
	self: AdminSmallGroupsHomePermissionsRestrictedState with SmallGroupServiceComponent with SecurityServiceComponent with SmallGroupSetWorkflowServiceComponent =>

	def applyInternal() = {
		val modules =
			if (canManageDepartment) department.modules.asScala
			else modulesWithPermission

		val sets =
			smallGroupService.getSmallGroupSets(department, academicYear)
				.filter { set => securityService.can(user, RequiredPermission, set) }

		val setViews = sets.map { set =>
			val progress = smallGroupSetWorkflowService.progress(set)

			ViewSetWithProgress(
				set = set,
				groups = set.groups.asScala,
				viewerRole = Tutor,
				progress = SetProgress(progress.percentage, progress.cssClass, progress.messageCode),
				nextStage = progress.nextStage,
				stages = progress.stages
			)
		}

		AdminSmallGroupsHomeInformation(
			canAdminDepartment = canManageDepartment,
			modulesWithPermission = modules.toSeq.sortBy { _.code },
			setsWithPermission = setViews
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
			if (!managedModules.isEmpty) p.PermissionCheckAll(RequiredPermission, managedModules)
			else p.PermissionCheck(RequiredPermission, department)
		}
	}
}
