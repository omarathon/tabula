package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.commands.assignments.ArchiveAssignmentCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Participate

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/archive"))
class ArchiveAssignmentController extends BaseController {

	@ModelAttribute def model(assignment:Assignment) = new ArchiveAssignmentCommand(assignment)
	
	@RequestMapping(method=Array(GET,HEAD))
	def confirmation(cmd:ArchiveAssignmentCommand, module:Module) = {
		check(cmd, module)
		Mav("admin/assignments/archive").noLayoutIf(ajax)
	}
	
	@RequestMapping(method=Array(POST))
	def apply(cmd:ArchiveAssignmentCommand, module:Module) = {
		check(cmd, module)
	}
	
	def check(cmd:ArchiveAssignmentCommand, module:Module) {
		mustBeLinked(cmd.assignment, module)
		mustBeAbleTo(Participate(module))
	}
	
}