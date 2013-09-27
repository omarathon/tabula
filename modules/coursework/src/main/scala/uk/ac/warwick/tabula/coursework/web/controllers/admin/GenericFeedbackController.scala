package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.coursework.commands.feedback.GenericFeedbackCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import scala.Array
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/generic"))
class GenericFeedbackController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		GenericFeedbackCommand(module, assignment)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: GenericFeedbackCommand, errors: Errors): Mav = {

		Mav("admin/assignments/feedback/generic_feedback",
			"command" -> command).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") @Valid command: GenericFeedbackCommand, errors: Errors): Mav = {
		command.apply()
		Mav("ajax_success").noLayout()
	}

}