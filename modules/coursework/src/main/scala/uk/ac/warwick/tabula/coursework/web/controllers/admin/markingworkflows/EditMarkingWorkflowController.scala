package uk.ac.warwick.tabula.coursework.web.controllers.admin.markingworkflows

import javax.validation.Valid
import scala.collection.JavaConversions._
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.tabula.coursework.commands.markingworkflows.EditMarkingWorkflowCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.MarkingWorkflowDao
import uk.ac.warwick.spring.Wire

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markingworkflows/edit/{markingworkflow}"))
class EditMarkingWorkflowController extends CourseworkController {

	// tell @Valid annotation how to validate
	validatesSelf[EditMarkingWorkflowCommand]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable("department") department: Department, @PathVariable("markingworkflow") markingWorkflow: MarkingWorkflow) =
		new EditMarkingWorkflowCommand(department, markingWorkflow)
	 
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: EditMarkingWorkflowCommand): Mav = {
		doBind(cmd)
		Mav("admin/markingworkflows/edit", "hasSubmissions" -> cmd.hasExistingSubmissions)
	}
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: EditMarkingWorkflowCommand, errors: Errors): Mav = {
		doBind(cmd)
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.markingWorkflow.list(cmd.department))
		}
	}
	
	// do extra property processing on the form.
	def doBind(cmd: EditMarkingWorkflowCommand) {
		cmd.doBind()
	}
	
}