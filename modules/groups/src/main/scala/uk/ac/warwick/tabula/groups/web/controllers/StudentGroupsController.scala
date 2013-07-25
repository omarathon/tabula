package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ModelAttribute}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.groups.commands.{ListStudentsGroupsCommandImpl}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewModules, ViewSet, ViewModule}

/**
 * Displays the groups that the current user is a member of.
 */
@Controller
@RequestMapping(Array("/student"))
class StudentGroupsController extends GroupsController {

	@ModelAttribute("command") def command(user: CurrentUser) =
		new ListStudentsGroupsCommandImpl(user)

	@RequestMapping(method = Array(POST))
	def listGroups(@ModelAttribute("command") command: ListStudentsGroupsCommandImpl): Mav = {
		val mapping = command.apply()

		val data = generateGroupsViewModel(mapping)

		Mav("groups/students_groups",
			"data" -> data
		).noLayoutIf(ajax)
	}

}
