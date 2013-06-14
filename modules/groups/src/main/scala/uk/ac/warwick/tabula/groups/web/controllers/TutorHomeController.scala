package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.groups.commands.{TutorHomeCommandImpl, TutorHomeCommand}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel

@Controller
class TutorHomeController extends GroupsController {

	@ModelAttribute("command") def command(user: CurrentUser) =
		new TutorHomeCommandImpl(user)

	@RequestMapping(Array("/tutor"))
	def listModules(@ModelAttribute("command") command: TutorHomeCommand): Mav = {
		val mapping = command.apply()

		// Build view model
		val moduleItems = mapping.toList map { case (module, sets) =>
			GroupsViewModel.ViewModule(
				module,
				sets map {GroupsViewModel.ViewSet(_)},
				canManage = false
			)
		}
		val data = GroupsViewModel.ViewModules( moduleItems )

		Mav("groups/tutor_home",
			"data" -> data
		)
	}

}
