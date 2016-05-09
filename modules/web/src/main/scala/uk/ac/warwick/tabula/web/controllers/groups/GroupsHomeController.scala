package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.groups.web.views.{GroupsDisplayHelper, GroupsViewModel}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModules
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.roles.SmallGroupSetViewerRoleDefinition
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent, SmallGroupService}

trait GroupsDepartmentsAndModulesWithPermission {

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

@Controller class GroupsHomeController extends GroupsController with GroupsDepartmentsAndModulesWithPermission
	with AutowiringModuleAndDepartmentServiceComponent {

	import GroupsDisplayHelper._

	var smallGroupService = Wire[SmallGroupService]

	@RequestMapping(Array("/groups")) def home(user: CurrentUser) = {
		if (user.loggedIn) {
			val departmentsAndRoutes = departmentsAndModulesForPermission(user, Permissions.Module.ManageSmallGroups)
			val taughtGroups = smallGroupService.findSmallGroupsByTutor(user.apparentUser)
			val memberGroupSets = smallGroupService.findSmallGroupSetsByMember(user.apparentUser)
			val releasedMemberGroupSets = getGroupSetsReleasedToStudents(memberGroupSets)
			val nonEmptyMemberViewModules = getViewModulesForStudent(releasedMemberGroupSets, getGroupsToDisplay(_,user.apparentUser))
			val todaysOccurrences = smallGroupService.findTodaysEventOccurrences(Seq(user.apparentUser), departmentsAndRoutes.modules.toSeq, departmentsAndRoutes.departments.toSeq)

			Mav("groups/home/view",
				"ownedDepartments" -> departmentsAndRoutes.departments,
				"ownedModuleDepartments" -> departmentsAndRoutes.modules.map { _.adminDepartment },
				"taughtGroups" -> taughtGroups,
				"memberGroupsetModules" -> ViewModules(nonEmptyMemberViewModules.sortBy(_.module.code), canManageDepartment = false),
				"todaysModules" -> ViewModules.fromOccurrences(todaysOccurrences, GroupsViewModel.Tutor),
				"showOccurrenceAttendance" -> true
			)
		} else Mav("groups/home/view")
	}

}