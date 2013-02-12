package uk.ac.warwick.tabula.coursework.web.controllers.admin.markingworkflows

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.tabula.coursework.commands.markingworkflows.AddMarkingWorkflowCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markingworkflows/add"))
class AddMarkingWorkflowController extends CourseworkController {

	// tell @Valid annotation how to validate
	validatesSelf[AddMarkingWorkflowCommand]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable("department") department: Department) = new AddMarkingWorkflowCommand(department)
	
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: AddMarkingWorkflowCommand): Mav = {
		doBind(cmd)
		Mav("admin/markingworkflows/add", "hasSubmissions" -> false)
	}
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: AddMarkingWorkflowCommand, errors: Errors): Mav = {
		doBind(cmd)
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.markingWorkflow.list(cmd.department))
		}
	}
	
	// do extra property processing on the form.
	def doBind(cmd: AddMarkingWorkflowCommand) = cmd.doBind()
	
}