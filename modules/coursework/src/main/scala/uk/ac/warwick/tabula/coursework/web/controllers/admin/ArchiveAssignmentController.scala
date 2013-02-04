package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.commands.assignments.ArchiveAssignmentCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/archive"))
class ArchiveAssignmentController extends CourseworkController {

	@ModelAttribute("command") def model(module: Module, assignment: Assignment) = new ArchiveAssignmentCommand(module, assignment)

	@RequestMapping(method = Array(GET, HEAD))
	def confirmation(@ModelAttribute("command") cmd: ArchiveAssignmentCommand) = {
		Mav("admin/assignments/archive").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST))
	def apply(@ModelAttribute("command") cmd: ArchiveAssignmentCommand) = {
		cmd.apply();
		Mav("ajax_success").noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}

}