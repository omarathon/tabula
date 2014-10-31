package uk.ac.warwick.tabula.coursework.web.controllers.admin.assignments

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.commands.assignments.{ArchiveAssignmentCommand, ArchiveAssignmentCommandInternal}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/archive"))
class ArchiveAssignmentController extends CourseworkController {

	@ModelAttribute("command") def model(module: Module, assignment: Assignment) =
		ArchiveAssignmentCommand(module, assignment)

	@RequestMapping(method = Array(GET, HEAD))
	def confirmation(@ModelAttribute("command") cmd: ArchiveAssignmentCommandInternal) = {
		Mav("admin/assignments/archive").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST))
	def apply(@ModelAttribute("command") cmd: Appliable[Assignment]) = {
		cmd.apply()
		Mav("ajax_success").noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}

}