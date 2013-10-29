package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{ RequestMapping, PathVariable, ModelAttribute }
import uk.ac.warwick.tabula.groups.commands.ViewSmallGroupAttendanceCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.groups.commands.admin.ViewSmallGroupSetAttendanceCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import scala.collection.immutable.SortedMap
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.groups.commands.admin.ViewModuleAttendanceCommand
import uk.ac.warwick.tabula.web.Breadcrumbs

@RequestMapping(Array("/admin/module/{module}/attendance"))
@Controller
class ViewModuleAttendanceController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module) = ViewModuleAttendanceCommand(module)

	@RequestMapping
	def show(@ModelAttribute("command") command: Appliable[SortedMap[SmallGroupSet, SortedMap[SmallGroup, ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation]]], @PathVariable module: Module): Mav = {
		val sets = command.apply()
		
		if (ajax) Mav("groups/attendance/view_module_partial", "sets" -> sets).noLayout()
		else 
			Mav("groups/attendance/view_module", 
				"sets" -> sets
			).crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

}
