package uk.ac.warwick.tabula.coursework.web.controllers.admin.markingworkflows

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.ReplaceMarkerInMarkingWorkflowCommand
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{MarkingWorkflow, Department}
import uk.ac.warwick.tabula.coursework.web.{Routes => CourseworkRoutes}

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markingworkflows/edit/{markingWorkflow}/replace"))
class ReplaceMarkerInMarkingWorkflowController extends CourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def cmd(@PathVariable department: Department, @PathVariable markingWorkflow: MarkingWorkflow) =
		ReplaceMarkerInMarkingWorkflowCommand(department, markingWorkflow)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MarkingWorkflow], @PathVariable department: Department) = {
		Mav("admin/markingworkflows/replace").crumbs(Breadcrumbs.Department(department))
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[MarkingWorkflow], errors: Errors,
		@PathVariable department: Department,
		@PathVariable markingWorkflow: MarkingWorkflow
	) = {
		if (errors.hasErrors) {
			form(cmd, department)
		} else {
			cmd.apply()
			Redirect(CourseworkRoutes.admin.markingWorkflow.edit(markingWorkflow))
		}
	}

}
