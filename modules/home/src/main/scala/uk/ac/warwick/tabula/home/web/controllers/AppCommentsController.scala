package uk.ac.warwick.tabula.home.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.tabula.home.commands.AppCommentCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.web.controllers.BaseController
import javax.validation.Valid

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