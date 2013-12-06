package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.{UnallocatedStudentsInformation, ListGroupUnallocatedStudentsCommand}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.Appliable


@Controller
@RequestMapping(value=Array("/{smallGroupSet}/unallocatedstudentspopup"))
class ListGroupUnallocatedStudentsController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable smallGroupSet: SmallGroupSet, user: CurrentUser) =
		ListGroupUnallocatedStudentsCommand(smallGroupSet, user)

	@RequestMapping
	def ajaxList(@ModelAttribute("command") command: Appliable[UnallocatedStudentsInformation]): Mav = {

		val info = command.apply()

		Mav("groups/students",
			"students" -> info.studentsNotInGroups,
			"userUniId" -> user.universityId,
			"userIsMember" -> info.userIsMember,
			"studentsCanSeeTutorName" -> info.showTutors
		).noLayout()
	}
}
