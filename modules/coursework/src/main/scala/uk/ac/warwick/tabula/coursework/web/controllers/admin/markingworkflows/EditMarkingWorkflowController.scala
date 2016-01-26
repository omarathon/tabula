package uk.ac.warwick.tabula.coursework.web.controllers.admin.markingworkflows

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.{EditMarkingWorkflowCommand, MarkingWorkflowCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.coursework.web.{Routes => CourseworkRoutes}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markingworkflows/edit/{markingworkflow}"))
class EditMarkingWorkflowController extends CourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def cmd(@PathVariable department: Department, @PathVariable markingworkflow: MarkingWorkflow) =
		EditMarkingWorkflowCommand(department, markingworkflow)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MarkingWorkflow] with MarkingWorkflowCommandState): Mav = {
		Mav("admin/markingworkflows/edit", "isExams" -> false).crumbs(Breadcrumbs.Department(cmd.department))
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[MarkingWorkflow] with MarkingWorkflowCommandState, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(CourseworkRoutes.admin.markingWorkflow.list(cmd.department))
		}
	}

}