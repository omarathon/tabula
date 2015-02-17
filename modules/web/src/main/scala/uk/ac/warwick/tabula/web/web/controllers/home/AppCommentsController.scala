package uk.ac.warwick.tabula.web.web.controllers.home

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.home.AppCommentCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

/**
 * App to receive comments/feedback about the app.
 */
@Controller
@RequestMapping(value = Array("/app/tell-us"))
class AppCommentsController extends BaseController {

	@ModelAttribute def command(user: CurrentUser) = new AppCommentCommand(user)

	validatesSelf[AppCommentCommand]

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute command: AppCommentCommand, errors: Errors): Mav = {
		formView
	}

	def formView = chooseLayout(Mav("app/comments/form"))

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute command: AppCommentCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			formView
		} else {
			command.apply()
			chooseLayout(Mav("app/comments/success"))
		}
	}

	private def chooseLayout(mav: Mav) = mav.noLayoutIf(ajax)

}