package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.groups.{TutorHomeCommand, TutorHomeCommandImpl}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.web.Mav

/**
 * Displays the group sets that the current user is a tutor of.
 */
@Controller
class GroupsTutorHomeController extends GroupsController {

	@ModelAttribute("command") def command(user: CurrentUser) =
		new TutorHomeCommandImpl(user)

	@RequestMapping(Array("/groups/tutor"))
	def listModules(
		@ModelAttribute("command") command: TutorHomeCommand,
		@RequestParam(value="updatedOccurrence", required=false) occurrence: SmallGroupEventOccurrence
	): Mav = {
		val mapping = command.apply()

		val data = GroupsViewModel.ViewModules.generate(mapping, GroupsViewModel.Tutor)

		Mav("groups/tutor_home",
			"data" -> data,
			"updatedOccurrence" -> occurrence,
			"ajax" -> ajax,
			"isSelf" -> true
		).noLayoutIf(ajax)
	}

}
