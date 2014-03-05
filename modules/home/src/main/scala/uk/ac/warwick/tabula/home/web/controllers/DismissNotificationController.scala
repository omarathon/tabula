package uk.ac.warwick.tabula.home.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestParam, ModelAttribute}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.home.commands.DismissNotificationCommand
import uk.ac.warwick.tabula.data.model.{ToEntityReference, Activity, Notification}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.views.MarkdownRendererImpl

@Controller
class DismissNotificationController extends BaseController with ActivityJsonMav with MarkdownRendererImpl {

	@ModelAttribute("dismissCommand")
	def dismissCommand(user: CurrentUser, @PathVariable notification: Notification[_ >: Null <: ToEntityReference ,_]) =
		DismissNotificationCommand(Seq(notification), dismiss=true, user.apparentUser)

	@ModelAttribute("restoreCommand")
	def restoreCommand(user: CurrentUser, @PathVariable notification: Notification[_ >: Null <: ToEntityReference ,_]) =
		DismissNotificationCommand(Seq(notification), dismiss=false, user.apparentUser)


	@RequestMapping(value=Array("/activity/dismiss/{notification}"))
	def dismiss(@ModelAttribute("dismissCommand") command: Appliable[Seq[Activity[_]]]) = {
		val activities = command.apply()
		toMav(activities)
	}

	@RequestMapping(value=Array("/activity/restore/{notification}"))
	def restore(@ModelAttribute("restoreCommand") command: Appliable[Seq[Activity[_]]]) = {
		val activities = command.apply()
		toMav(activities)
	}
}
