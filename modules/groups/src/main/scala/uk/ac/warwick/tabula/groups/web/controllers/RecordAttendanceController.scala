package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.groups.commands.RecordAttendanceCommand
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{RequestMapping, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent

@RequestMapping(value=Array("/event/{event}/register/{week}"))
@Controller
class RecordAttendanceController extends GroupsController {

	@ModelAttribute
	def command(
			@PathVariable event: SmallGroupEvent,
			@PathVariable week: Int
		) = RecordAttendanceCommand(event, week)

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute command: RecordAttendanceCommand): Mav = {
		Mav("groups/attendance/form")
	}

}
