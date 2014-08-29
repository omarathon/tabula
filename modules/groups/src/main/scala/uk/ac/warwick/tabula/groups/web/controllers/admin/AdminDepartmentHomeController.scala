package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel

import scala.collection.JavaConverters._

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSetFilters, SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.groups.commands.admin.{AdminSmallGroupsHomeInformation, AdminSmallGroupsHomeCommand}
import uk.ac.warwick.tabula.commands.Appliable

abstract class AbstractAdminDepartmentHomeController extends GroupsController {
	type AdminSmallGroupsHomeCommand = Appliable[AdminSmallGroupsHomeInformation]

	hideDeletedItems

	@ModelAttribute("academicYears") def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2).asJava

	@ModelAttribute("allocated") def allocatedSet(@RequestParam(value="allocated", required=false) set: SmallGroupSet) = set

	@RequestMapping
	def adminDepartment(@ModelAttribute("adminCommand") cmd: AdminSmallGroupsHomeCommand, @PathVariable("department") department: Department, user: CurrentUser) = {
		val info = cmd.apply()

		val hasModules = info.modulesWithPermission.nonEmpty
		val hasGroups = info.setsWithPermission.nonEmpty
		val hasGroupAttendance = info.setsWithPermission.exists { _.set.showAttendanceReports }

		val model = Map(
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
			"allAllocationFilters" -> SmallGroupSetFilters.AllocationMethod.all(info.departmentSmallGroupSets)
		)

		if (ajax) Mav("admin/department-noLayout", model).noLayout()
		else Mav("admin/department", model)
	}
}

@Controller
@RequestMapping(value=Array("/admin/department/{department}"))
class AdminDepartmentHomeController extends AbstractAdminDepartmentHomeController {

	@ModelAttribute("adminCommand") def command(@PathVariable("department") dept: Department, user: CurrentUser): AdminSmallGroupsHomeCommand =
		AdminSmallGroupsHomeCommand(mandatory(dept), AcademicYear.guessSITSAcademicYearByDate(DateTime.now), user)

}

@Controller
@RequestMapping(value=Array("/admin/department/{department}/{academicYear}"))
class AdminDepartmentHomeForYearController extends AbstractAdminDepartmentHomeController {

	@ModelAttribute("adminCommand") def command(@PathVariable("department") dept: Department, @PathVariable("academicYear") academicYear: AcademicYear, user: CurrentUser): AdminSmallGroupsHomeCommand =
		AdminSmallGroupsHomeCommand(mandatory(dept), mandatory(academicYear), user)

}