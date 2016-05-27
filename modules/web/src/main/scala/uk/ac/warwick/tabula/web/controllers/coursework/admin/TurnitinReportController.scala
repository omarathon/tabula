package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.commands.coursework.turnitin.{TurnitinReportErrorWithMessage, ViewPlagiarismReportCommand}
import uk.ac.warwick.tabula.web.controllers.coursework.CourseworkController
import uk.ac.warwick.tabula.data.model.{FileAttachment, Assignment, Module}
import uk.ac.warwick.tabula.web.Mav

/**
 * Provides access to the Turnitin Document Viewer for a submission
 * that's been submitted to Turnitin.
 *
 * Supports both LTI and non-LTI versions
 */
@Controller
@RequestMapping(value = Array(
	"/coursework/admin/module/{module}/assignments/{assignment}/turnitin/report/{attachment}",
	"/coursework/admin/module/{module}/assignments/{assignment}/turnitin/lti-report/{attachment}"
))
class TurnitinReportController extends CourseworkController {

	type ViewPlagiarismReportCommand = ViewPlagiarismReportCommand.CommandType

	@ModelAttribute("command") def command(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable attachment: FileAttachment
	): ViewPlagiarismReportCommand = ViewPlagiarismReportCommand(module, assignment, attachment, user)

	@RequestMapping
	def goToReport(@ModelAttribute("command") command: ViewPlagiarismReportCommand): Mav = command.apply() match {
		case Left(uri) =>
			if (command.ltiParams.nonEmpty)	Mav("coursework/admin/assignments/turnitin/lti_report_forward", "turnitin_report_url" -> uri, "params" -> command.ltiParams)
			else Mav("redirect:" + uri.toString)

		case Right(error: TurnitinReportErrorWithMessage) =>
			Mav("coursework/admin/assignments/turnitin/report_error", "problem" -> error.code, "message" -> error.message)

		case Right(error) =>
			Mav("coursework/admin/assignments/turnitin/report_error", "problem" -> error.code)
	}

}