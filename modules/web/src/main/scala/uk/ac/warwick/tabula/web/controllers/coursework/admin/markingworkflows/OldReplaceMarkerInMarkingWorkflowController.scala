package uk.ac.warwick.tabula.web.controllers.coursework.admin.markingworkflows

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.ReplaceMarkerInMarkingWorkflowCommand
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Department, MarkingWorkflow}
import uk.ac.warwick.tabula.coursework.web.{Routes => CourseworkRoutes}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/coursework/admin/department/{department}/markingworkflows/edit/{markingWorkflow}/replace"))
class OldReplaceMarkerInMarkingWorkflowController extends OldCourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def cmd(@PathVariable department: Department, @PathVariable markingWorkflow: MarkingWorkflow) =
		ReplaceMarkerInMarkingWorkflowCommand(department, markingWorkflow)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MarkingWorkflow], @PathVariable department: Department) = {
		Mav("coursework/admin/markingworkflows/replace").crumbs(Breadcrumbs.Department(department))
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
