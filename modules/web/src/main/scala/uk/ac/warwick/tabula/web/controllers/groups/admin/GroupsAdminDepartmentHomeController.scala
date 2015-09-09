package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

import scala.collection.JavaConverters._

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSetFilters, SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.commands.groups.admin.{AdminSmallGroupsHomeCommandState, AdminSmallGroupsHomeInformation, AdminSmallGroupsHomeCommand}
import uk.ac.warwick.tabula.commands.Appliable

abstract class AbstractGroupsAdminDepartmentHomeController extends GroupsController with AutowiringTermServiceComponent {
	type AdminSmallGroupsHomeCommand = Appliable[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState

	hideDeletedItems

	@ModelAttribute("academicYears") def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2).asJava

	@ModelAttribute("allocated") def allocatedSet(@RequestParam(value="allocated", required=false) set: SmallGroupSet) = set

	private def process(cmd: AdminSmallGroupsHomeCommand, department: Department, view: String) = {
		val info = cmd.apply()

		val hasModules = info.modulesWithPermission.nonEmpty
		val hasGroups = info.setsWithPermission.nonEmpty
		val hasGroupAttendance = info.setsWithPermission.exists { _.set.showAttendanceReports }
		val isFiltered = !(cmd.moduleFilters.isEmpty && cmd.statusFilters.isEmpty && cmd.allocationMethodFilters.isEmpty && cmd.termFilters.isEmpty)

		val model = Map(
			"viewedAcademicYear" -> cmd.academicYear,
			"department" -> department,
			"canAdminDepartment" -> info.canAdminDepartment,
			"modules" -> info.modulesWithPermission,
			"sets" -> info.setsWithPermission,
			"hasUnreleasedGroupsets" -> info.setsWithPermission.exists { !_.set.fullyReleased },
			"hasOpenableGroupsets" -> info.setsWithPermission.exists { sv => (!sv.set.openForSignups) && sv.set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp },
			"hasCloseableGroupsets" -> info.setsWithPermission.exists { sv => sv.set.openForSignups && sv.set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp },
			"hasModules" -> hasModules,
			"hasGroups" -> hasGroups,
			"hasGroupAttendance" -> hasGroupAttendance,
			"allStatusFilters" -> SmallGroupSetFilters.Status.all,
			"allModuleFilters" -> SmallGroupSetFilters.allModuleFilters(info.modulesWithPermission),
			"allAllocationFilters" -> SmallGroupSetFilters.AllocationMethod.all(info.departmentSmallGroupSets),
			"allTermFilters" -> SmallGroupSetFilters.allTermFilters(cmd.academicYear, termService),
			"isFiltered" -> isFiltered
		)

		Mav(view, model)
	}

	@RequestMapping(params=Array("!ajax"), headers=Array("!X-Requested-With"))
	def adminDepartment(@ModelAttribute("adminCommand") cmd: AdminSmallGroupsHomeCommand, @PathVariable("department") department: Department, user: CurrentUser) =
		process(cmd, department, "groups/admin/department")

	@RequestMapping
	def loadSets(@ModelAttribute("adminCommand") cmd: AdminSmallGroupsHomeCommand, @PathVariable("department") department: Department, user: CurrentUser) =
		process(cmd, department, "groups/admin/department-noLayout").noLayout()
}

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}"))
class GroupsAdminDepartmentController extends AbstractGroupsAdminDepartmentHomeController {

	@ModelAttribute("adminCommand") def command(@PathVariable("department") dept: Department, user: CurrentUser): AdminSmallGroupsHomeCommand =
		AdminSmallGroupsHomeCommand(mandatory(dept), AcademicYear.guessSITSAcademicYearByDate(DateTime.now), user, calculateProgress = ajax)

}

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}/{academicYear}"))
class GroupsAdminDepartmentForYearController extends AbstractGroupsAdminDepartmentHomeController {

	@ModelAttribute("adminCommand") def command(@PathVariable("department") dept: Department, @PathVariable("academicYear") academicYear: AcademicYear, user: CurrentUser): AdminSmallGroupsHomeCommand =
		AdminSmallGroupsHomeCommand(mandatory(dept), mandatory(academicYear), user, calculateProgress = ajax)

}