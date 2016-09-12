package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Assignment, Mark, Module}
import uk.ac.warwick.tabula.commands.exams.{BulkAdjustmentCommand, BulkAdjustmentTemplateCommand}
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.views.ExcelView

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/coursework/admin/module/{module}/assignments/{assignment}/feedback/bulk-adjustment"))
class OldBulkFeedbackAdjustmentController extends OldCourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		BulkAdjustmentCommand(
			mandatory(assignment),
			GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)),
			SpreadsheetHelpers,
			user
		)

	@RequestMapping(method = Array(GET, HEAD))
	def form = {
		Mav("coursework/admin/assignments/feedback/bulk/bulk_adjustment",
			"StudentIdHeader" -> BulkAdjustmentCommand.StudentIdHeader,
			"MarkHeader" -> BulkAdjustmentCommand.MarkHeader,
			"GradeHeader" -> BulkAdjustmentCommand.GradeHeader
		)
	}

	@RequestMapping(method = Array(POST))
	def upload(@Valid @ModelAttribute("command") cmd: Appliable[Seq[Mark]], errors: Errors) = {
		if (errors.hasFieldErrors("file"))
			form
		else
			Mav("coursework/admin/assignments/feedback/bulk/preview")
	}

	@RequestMapping(method = Array(POST), params = Array("confirmStep=true"))
	def confirm(
		 @Valid @ModelAttribute("command") cmd: Appliable[Seq[Mark]], errors: Errors,
		 @PathVariable assignment: Assignment
	) = {
		if (errors.hasFieldErrors("defaultReason") || errors.hasFieldErrors("defaultComment")) {
			upload(cmd, errors)
		} else {
			cmd.apply()
			Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/coursework/admin/module/{module}/assignments/{assignment}/feedback/bulk-adjustment/template"))
class BulkFeedbackAdjustmentTemplateController extends OldCourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment): Appliable[ExcelView] =
		BulkAdjustmentTemplateCommand(mandatory(assignment))

	@RequestMapping(method = Array(GET, HEAD))
	def home(@ModelAttribute("command") cmd: Appliable[ExcelView]) = {
		cmd.apply()
	}

}