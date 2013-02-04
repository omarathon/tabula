package uk.ac.warwick.tabula.coursework.web.controllers.admin
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.commands.feedback.PublishFeedbackCommand
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment

@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/publish"))
@Controller
class PublishFeedbackController extends CourseworkController {
	
	@ModelAttribute def cmd(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new PublishFeedbackCommand(module, assignment)

	@RequestMapping(method = Array(HEAD, GET), params = Array("!confirm"))
	def confirmation(command: PublishFeedbackCommand, errors: Errors): Mav = {
		if (errors.hasErrors) command.prevalidate(errors)
		Mav("admin/assignments/publish/form",
			"assignment" -> command.assignment)
	}

	@RequestMapping(method = Array(POST))
	def submit(command: PublishFeedbackCommand, errors: Errors): Mav = {
		command.validate(errors)
		if (errors.hasErrors()) {
			confirmation(command, errors)
		} else {
			command.apply
			Mav("admin/assignments/publish/done", "assignment" -> command.assignment)
		}
	}

}