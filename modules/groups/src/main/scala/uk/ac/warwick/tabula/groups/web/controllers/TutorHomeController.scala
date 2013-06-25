package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.groups.commands.{TutorHomeCommandImpl, TutorHomeCommand}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel._

/**
 * Displays the group sets that the current user is a tutor of.
 */
@Controller
class TutorHomeController extends GroupsController {

	@ModelAttribute("command") def command(user: CurrentUser) =
		new TutorHomeCommandImpl(user)

	@RequestMapping(Array("/tutor"))
	def listModules(@ModelAttribute("command") command: TutorHomeCommand): Mav = {
		val mapping = command.apply()

		// Build the view model
		val moduleItems =
			for ((module, sets) <- mapping) yield {
				ViewModule(module,
					sets map { ViewSet(_, None) },
					None
				)
			}
		val data = ViewModules( moduleItems.toSeq )

		Mav("groups/tutor_home",
			"data" -> data
		)
	}

}
