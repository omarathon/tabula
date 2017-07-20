package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAttemptCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/submission/{assignment}/attempt"))
class SubmissionAttemptController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) =
		SubmissionAttemptCommand(mandatory(assignment), user)

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") cmd: Appliable[Unit]): Mav = {
		cmd.apply()
		Mav(new JSONView(Map(
			"success" -> true
		)))
	}

}
