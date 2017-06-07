package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.commands.exams.exams.{BulkAdjustmentCommand, BulkAdjustmentTemplateCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Assignment, Mark}
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.ExcelView

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/bulk-adjustment"))
class BulkFeedbackAdjustmentController extends CourseworkController {

	type BulkAdjustmentCommand = Appliable[Seq[Mark]]
	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) =
		BulkAdjustmentCommand(
			mandatory(assignment),
			GenerateGradesFromMarkCommand(mandatory(assignment)),
			SpreadsheetHelpers,
			user
		)


	@RequestMapping
	def form(
		@PathVariable assignment: Assignment
	): Mav = {
		Mav("cm2/admin/assignments/feedback/bulk/bulk_adjustment",
			"StudentIdHeader" -> BulkAdjustmentCommand.StudentIdHeader,
			"MarkHeader" -> BulkAdjustmentCommand.MarkHeader,
			"GradeHeader" -> BulkAdjustmentCommand.GradeHeader
		).crumbsList(Breadcrumbs.assignment(assignment))
	}

	@RequestMapping(method = Array(POST), params = Array("!confirmStep"))
	def upload(@Valid @ModelAttribute("command") cmd: BulkAdjustmentCommand, @PathVariable assignment: Assignment, errors: Errors): Mav = {
		if (errors.hasFieldErrors("file"))
			form(assignment)
		else
			Mav("cm2/admin/assignments/feedback/bulk/preview").crumbsList(Breadcrumbs.assignment(assignment))
	}

	@RequestMapping(method = Array(POST), params = Array("confirmStep=true"))
	def confirm(
		@Valid @ModelAttribute("command") cmd: BulkAdjustmentCommand, errors: Errors,
		@PathVariable assignment: Assignment
	): Mav = {
		if (errors.hasFieldErrors("defaultReason") || errors.hasFieldErrors("defaultComment")) {
			upload(cmd, assignment, errors)
		} else {
			cmd.apply()
			Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/bulk-adjustment/template"))
class BulkFeedbackAdjustmentTemplateController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment): Appliable[ExcelView] =
		BulkAdjustmentTemplateCommand(mandatory(assignment))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[ExcelView]): ExcelView = {
		cmd.apply()
	}

}