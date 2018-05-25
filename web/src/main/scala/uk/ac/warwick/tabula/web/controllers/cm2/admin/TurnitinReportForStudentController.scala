package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.turnitin.{TurnitinReportErrorWithMessage, ViewPlagiarismReportCommand}
import uk.ac.warwick.tabula.data.model.{FileAttachment, Submission}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

/**
 * Provides access to the Turnitin Document Viewer for a student
 * for a submission that's been submitted to Turnitin.
 *
 * Supports LTI and cm2 only atm.
 */
@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value = Array(
	"/${cm2.prefix}/submission/{submission}/turnitin/lti-report/{attachment}"
))
class TurnitinReportForStudentController extends CourseworkController {

	type ViewPlagiarismReportCommand = ViewPlagiarismReportCommand.CommandType

	@ModelAttribute("command") def command(
		@PathVariable submission: Submission,
		@PathVariable attachment: FileAttachment
	): ViewPlagiarismReportCommand = ViewPlagiarismReportCommand(mandatory(submission.assignment), mandatory(attachment), isInstructor = false, user)

	@RequestMapping
	def goToReport(@ModelAttribute("command") command: ViewPlagiarismReportCommand): Mav = command.apply() match {
		case Left(uri) =>
			if (command.ltiParams.nonEmpty)	Mav("cm2/admin/assignments/turnitin/lti_report_forward", "turnitin_report_url" -> uri, "params" -> command.ltiParams)
			else Mav("redirect:" + uri.toString)

		case Right(error: TurnitinReportErrorWithMessage) =>
			Mav("cm2/admin/assignments/turnitin/report_error", "problem" -> error.code, "message" -> error.message)

		case Right(error) =>
			Mav("cm2/admin/assignments/turnitin/report_error", "problem" -> error.code)
	}

}