package uk.ac.warwick.tabula.coursework.web.controllers.admin.markingworkflows

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.AddMarkingWorkflowCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.MarkingWorkflowCommandState

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markingworkflows/add"))
class AddMarkingWorkflowController extends CourseworkController {

	// tell @Valid annotation how to validate
	validatesSelf[SelfValidating]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable("department") department: Department) = AddMarkingWorkflowCommand(department)
	
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MarkingWorkflow] with MarkingWorkflowCommandState): Mav = {
		Mav("admin/markingworkflows/add").crumbs(Breadcrumbs.Department(cmd.department))
	}

	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[MarkingWorkflow] with MarkingWorkflowCommandState, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.markingWorkflow.list(cmd.department))
		}
	}
	
}