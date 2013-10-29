package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.groups.commands.RecordAttendanceCommand
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{ RequestMapping, PathVariable, ModelAttribute }
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import org.springframework.validation.Errors
import javax.validation.Valid
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.groups.commands.ViewSmallGroupAttendanceCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.commands.Appliable

@RequestMapping(value = Array("/group/{group}/attendance"))
@Controller
class ViewSmallGroupAttendanceController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable group: SmallGroup) = ViewSmallGroupAttendanceCommand(group)

	@RequestMapping
	def show(@ModelAttribute("command") command: Appliable[ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation], @PathVariable group: SmallGroup): Mav = {
		val attendanceInfo = command.apply()
		val module = group.groupSet.module
		
		Mav("groups/attendance/view_group", 
			"instances" -> attendanceInfo.instances,
			"studentAttendance" -> attendanceInfo.attendance
		).crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

}
