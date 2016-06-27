package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.{TimetableClashStudentsInformation, ListSmallGroupSetTimetableClashStudentsCommand, SmallGroupSetTimetableClashCommand, TimetableClashInformation}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(value = Array("/groups/{smallGroupSet}/timetableclash"))
class SmallGroupSetTimetableClashController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable smallGroupSet: SmallGroupSet, user: CurrentUser) =
		SmallGroupSetTimetableClashCommand(smallGroupSet, user)

	@RequestMapping
	def ajaxList(@ModelAttribute("command") command: Appliable[TimetableClashInformation]): Mav = {
		Mav(new JSONView(Map("students" -> command.apply().timtableClashMembersPerGroup)))
	}
}

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




