package uk.ac.warwick.tabula.web.controllers.exams.admin.markingworkflows

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.{DeleteMarkingWorkflowCommand, DeleteMarkingWorkflowCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Department, MarkingWorkflow}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(value=Array("/exams/admin/department/{department}/markingworkflows/delete/{markingWorkflow}"))
class ExamsDeleteMarkingWorkflowController extends ExamsController {

	type DeleteMarkingWorkflowCommand = Appliable[Unit]	with SelfValidating with DeleteMarkingWorkflowCommandState

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def cmd(@PathVariable("department") department: Department, @PathVariable("markingWorkflow") markingWorkflow: MarkingWorkflow): DeleteMarkingWorkflowCommand =
		DeleteMarkingWorkflowCommand(department, markingWorkflow)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: DeleteMarkingWorkflowCommand): Mav = {
		Mav("exams/admin/markingworkflows/delete").noLayoutIf(ajax)
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
