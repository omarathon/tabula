package uk.ac.warwick.courses.web.controllers

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod._

import uk.ac.warwick.courses.commands.AppCommentCommand
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.CurrentUser

/**
 * App to receive comments/feedback about the app.
 */
@Controller
@RequestMapping(value=Array("/app/tell-us"))
class AppCommentsController extends BaseController {
	
	@ModelAttribute def command(user:CurrentUser) = new AppCommentCommand(user)
	
	@RequestMapping(method=Array(GET, HEAD))
	def form (command:AppCommentCommand, errors:Errors) : Mav = {
		command.prefill
		formView
	}
	
	def formView = chooseLayout(Mav("app/comments/form"))
	
	@RequestMapping(method=Array(POST))
	def submit (command:AppCommentCommand, errors:Errors) : Mav = {
		command validate errors
		if (errors hasErrors) {
			formView
		} else {
			command.apply()
			chooseLayout(Mav("app/comments/success"))
		}
	}
	
	private def chooseLayout(mav:Mav) = mav.noLayoutIf(ajax)

}