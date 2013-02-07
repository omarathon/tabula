package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.coursework.commands.assignments.AssignMarkersCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.web.Routes
import scala.Array
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/assign-markers"))
class AssignMarkersController extends CourseworkController {

	@ModelAttribute def form(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new AssignMarkersCommand(module, assignment)

	@RequestMapping()
	def form(user: CurrentUser, @PathVariable assignment: Assignment, cmd: AssignMarkersCommand, errors: Errors) = {
		cmd.onBind()
		Mav("admin/assignments/assignmarkers/form")
	}

	@RequestMapping(method = Array(POST))
	def doUpload(@PathVariable module: Module, @PathVariable(value = "assignment") assignment: Assignment,
	             @ModelAttribute cmd: AssignMarkersCommand, errors: Errors) = {
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

}
