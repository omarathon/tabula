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

@RequestMapping(Array("/admin/module/{module}/groups/{set}/attendance"))
@Controller
class ViewSmallGroupSetAttendanceController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable set: SmallGroupSet) = ViewSmallGroupSetAttendanceCommand(set)

	@RequestMapping
	def show(@ModelAttribute("command") command: Appliable[SortedMap[SmallGroup, ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation]], @PathVariable set: SmallGroupSet): Mav = {
		val groups = command.apply()
		val module = set.module
		
		Mav("groups/attendance/view_set", 
			"groups" -> groups
		).crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

}
