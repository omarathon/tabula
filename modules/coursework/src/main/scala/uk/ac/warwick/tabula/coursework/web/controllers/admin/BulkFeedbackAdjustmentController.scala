package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Mark, Assignment, Module}
import uk.ac.warwick.tabula.exams.commands.{BulkAdjustmentTemplateCommand, BulkAdjustmentCommand}
import uk.ac.warwick.tabula.coursework.commands.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/bulk-adjustment"))
class BulkFeedbackAdjustmentController extends CourseworkController {

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
		Mav("admin/assignments/feedback/bulk/bulk_adjustment",
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
			Mav("admin/assignments/feedback/bulk/preview")
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

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/bulk-adjustment/template"))
class BulkFeedbackAdjustmentTemplateController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment): Appliable[ExcelView] =
		BulkAdjustmentTemplateCommand(mandatory(assignment))

	@RequestMapping(method = Array(GET, HEAD))
	def home(@ModelAttribute("command") cmd: Appliable[ExcelView]) = {
		cmd.apply()
	}

}