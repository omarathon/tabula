package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSetSelfSignUpState, SmallGroupSet}
import uk.ac.warwick.tabula.commands.groups.admin.{OpenSmallGroupSetCommand, OpenSmallGroupSetState}
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

@RequestMapping(Array("/groups/admin/module/{module}/groups/{set}/selfsignup/{action}"))
@Controller
class OpenSmallGroupSetController extends GroupsController {

	@ModelAttribute("openGroupSetCommand")
	def getOpenGroupSetCommand(
		@PathVariable("module") module: Module,
		@PathVariable("set") set: SmallGroupSet,
		@PathVariable action: SmallGroupSetSelfSignUpState
	): Appliable[Seq[SmallGroupSet]] with OpenSmallGroupSetState =
		OpenSmallGroupSetCommand(module.adminDepartment, Seq(set), user.apparentUser, action)

	@RequestMapping
	def form(@ModelAttribute("openGroupSetCommand") cmd: Appliable[Seq[SmallGroupSet]]) =
		Mav("groups/admin/groups/open").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("openGroupSetCommand") cmd: Appliable[Seq[SmallGroupSet]]) = {
		cmd.apply()
		Mav("ajax_success").noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}
}
