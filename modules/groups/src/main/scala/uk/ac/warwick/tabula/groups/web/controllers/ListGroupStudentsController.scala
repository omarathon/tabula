package uk.ac.warwick.tabula.groups.web.controllers

import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, ModelAttribute}
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.groups.commands.ListGroupStudentsCommand



@Controller
@RequestMapping(value=Array("/group/{group}/studentspopup"))
class ListGroupStudentsController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable group: SmallGroup) =
		new ListGroupStudentsCommand(group)

	@RequestMapping
	def ajaxList(@ModelAttribute("command") command: ListGroupStudentsCommand): Mav = {
		Mav("groups/students", "students" -> command.apply()).noLayout()
	}

}
