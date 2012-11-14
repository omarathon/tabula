package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import uk.ac.warwick.tabula.coursework.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.commands.assignments.ArchiveAssignmentCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.data.model.Module
import uk.ac.warwick.tabula.coursework.actions.Participate

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/archive"))
class ArchiveAssignmentController extends BaseController {

	@ModelAttribute("command") def model(assignment: Assignment) = new ArchiveAssignmentCommand(assignment)

	@RequestMapping(method = Array(GET, HEAD))
	def confirmation(@ModelAttribute("command") cmd: ArchiveAssignmentCommand, module: Module) = {
		check(cmd, module)
		Mav("admin/assignments/archive").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST))
	def apply(@ModelAttribute("command") cmd: ArchiveAssignmentCommand, module: Module) = {
		check(cmd, module)
		cmd.apply();
		Mav("ajax_success").noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}

	def check(cmd: ArchiveAssignmentCommand, module: Module) {
		mustBeLinked(cmd.assignment, module)
		mustBeAbleTo(Participate(module))
	}

}