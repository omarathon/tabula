package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.profiles.commands.ViewSmallGroupCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.CurrentUser

/**
 * Displays the students on a small group event of which you are a tutor.
 */
@Controller
class ViewSmallGroupController {

	@ModelAttribute def command(user: CurrentUser, @PathVariable smallGroup: SmallGroup) =
		new ViewSmallGroupCommand(user, smallGroup)

	@RequestMapping(value=Array("/groups/{smallGroup}/view"))
	def show(@ModelAttribute command: ViewSmallGroupCommand): Mav = {
		Mav("groups/view",
			"smallGroup" -> command.smallGroup,
			"tutees" -> command.apply()
		)
	}

}
