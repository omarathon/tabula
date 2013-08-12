package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.groups.commands.{TutorHomeCommandImpl, TutorHomeCommand}
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel

/**
 * Displays the group sets that the current user is a tutor of.
 */
@Controller
class TutorHomeController extends GroupsController {

	@ModelAttribute("command") def command(user: CurrentUser) =
		new TutorHomeCommandImpl(user)

	@RequestMapping(Array("/tutor"))
	def listModules(@ModelAttribute("command") command: TutorHomeCommand, @RequestParam(value="updatedOccurrence", required=false) occurrence: SmallGroupEventOccurrence): Mav = {
		val mapping = command.apply()

		val data = generateViewModules(mapping, GroupsViewModel.Tutor)

		Mav("groups/tutor_home",
			"data" -> data,
			"updatedOccurrence" -> occurrence
		)
	}

}
