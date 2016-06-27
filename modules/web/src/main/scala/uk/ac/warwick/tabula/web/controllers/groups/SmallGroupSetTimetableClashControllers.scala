package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.{TimetableClashStudentsInformation, ListSmallGroupSetTimetableClashStudentsCommand, SmallGroupSetTimetableClashCommand, TimetableClashInformation}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value=Array("/groups/{smallGroupSet}/timetableclashstudentspopup"))
class ListSmallGroupSetTimetableClashStudentsController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable smallGroupSet: SmallGroupSet, user: CurrentUser, @RequestParam(defaultValue="") usercodes: Array[String]) =
		ListSmallGroupSetTimetableClashStudentsCommand(smallGroupSet, user, usercodes)

	@RequestMapping
	def ajaxList(@ModelAttribute("command") command: Appliable[TimetableClashStudentsInformation]): Mav = {
		Mav("groups/timetableconflicts_students",
			"students" -> command.apply().timetableClashMembers
		).noLayout()
	}
}




