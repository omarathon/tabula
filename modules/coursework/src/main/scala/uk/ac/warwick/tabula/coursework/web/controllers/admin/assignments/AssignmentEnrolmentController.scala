package uk.ac.warwick.tabula.coursework.web.controllers.admin.assignments

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.WebDataBinder

import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{UpstreamGroupPropertyEditor, UpstreamGroup}
import uk.ac.warwick.tabula.helpers.Logging


/**
 * Controller to populate the user listing for editing, without persistence
 */
@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/enrolment"))
class AssignmentEnrolmentController extends CourseworkController with Logging{

	validatesSelf[EditAssignmentEnrolmentCommand]

	@ModelAttribute def formObject(@PathVariable("module") module: Module) = {
		val cmd = new EditAssignmentEnrolmentCommand(mandatory(module))
		cmd.upstreamGroups.clear()
		cmd
	}

	@RequestMapping
	def showForm(form: EditAssignmentEnrolmentCommand, openDetails: Boolean = false) = {
		form.afterBind()

		logger.info(s"Assignment Enrolment includeCount: ${form.membershipInfo.includeCount}")
		Mav("admin/assignments/enrolment",
			"department" -> form.module.department,
			"module" -> form.module,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"openDetails" -> openDetails)
			.noLayout()
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}

