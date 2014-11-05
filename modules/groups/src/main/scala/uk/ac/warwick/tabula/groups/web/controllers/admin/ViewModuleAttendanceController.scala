package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{ RequestMapping, PathVariable, ModelAttribute }
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import scala.collection.immutable.SortedMap
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.groups.commands.admin.ViewModuleAttendanceCommand

abstract class AbstractViewModuleAttendanceController extends GroupsController {

	@RequestMapping
	def show(
		@ModelAttribute("command") command:
		Appliable[SortedMap[SmallGroupSet, SortedMap[SmallGroup, ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation]]],
		@PathVariable module: Module
	): Mav = {
		val sets = command.apply()

		if (ajax)
			Mav("groups/attendance/view_module_partial",
				"sets" -> sets
			).noLayout()
		else
			Mav("groups/attendance/view_module",
				"sets" -> sets
			).crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))
	}

}

@RequestMapping(Array("/admin/module/{module}/attendance"))
@Controller
class ViewModuleAttendanceController extends AbstractViewModuleAttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module) =
		ViewModuleAttendanceCommand(mandatory(module), AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

}

@RequestMapping(Array("/admin/module/{module}/{academicYear}/attendance"))
@Controller
class ViewModuleAttendanceInYearController extends AbstractViewModuleAttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable academicYear: AcademicYear) =
		ViewModuleAttendanceCommand(mandatory(module), mandatory(academicYear))

}
