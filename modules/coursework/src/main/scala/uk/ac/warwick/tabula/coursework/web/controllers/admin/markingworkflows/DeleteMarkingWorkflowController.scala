package uk.ac.warwick.tabula.coursework.web.controllers.admin.markingworkflows

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.tabula.coursework.commands.markingworkflows.DeleteMarkingWorkflowCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markingworkflows/delete/{markingworkflow}"))
class DeleteMarkingWorkflowController extends CourseworkController {

	// tell @Valid annotation how to validate
	validatesSelf[DeleteMarkingWorkflowCommand]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable("department") department: Department, @PathVariable("markingworkflow") markingWorkflow: MarkingWorkflow) =
		new DeleteMarkingWorkflowCommand(department, markingWorkflow)
	
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: DeleteMarkingWorkflowCommand): Mav = {
		Mav("admin/markingworkflows/delete").noLayoutIf(ajax)
	}
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: DeleteMarkingWorkflowCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.markingWorkflow.list(cmd.department))
		}
	}
	
}