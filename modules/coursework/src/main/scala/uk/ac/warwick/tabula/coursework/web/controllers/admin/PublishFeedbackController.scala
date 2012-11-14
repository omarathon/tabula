package uk.ac.warwick.tabula.coursework.web.controllers.admin
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import uk.ac.warwick.tabula.coursework.web.Mav
import uk.ac.warwick.tabula.coursework.commands.feedback.PublishFeedbackCommand
import uk.ac.warwick.tabula.coursework.actions.Participate
import org.springframework.validation.Errors

@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/publish"))
@Controller
class PublishFeedbackController extends BaseController {

	private def check(command: PublishFeedbackCommand) {
		mustBeLinked(mandatory(command.assignment), mandatory(command.module))
		mustBeAbleTo(Participate(command.assignment.module))
	}

	@RequestMapping(method = Array(HEAD, GET), params = Array("!confirm"))
	def confirmation(command: PublishFeedbackCommand, errors: Errors): Mav = {
		check(command)
		if (errors.hasErrors) command.prevalidate(errors)
		Mav("admin/assignments/publish/form",
			"assignment" -> command.assignment)
	}

	@RequestMapping(method = Array(POST))
	def submit(command: PublishFeedbackCommand, errors: Errors): Mav = {
		check(command)
		command.validate(errors)
		if (errors.hasErrors()) {
			confirmation(command, errors)
		} else {
			command.apply
			Mav("admin/assignments/publish/done", "assignment" -> command.assignment)
		}
	}

}