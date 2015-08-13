package uk.ac.warwick.tabula.coursework.web.controllers.admin.modules

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ModelAttribute, PathVariable}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{FileAttachment, Assignment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.coursework.turnitinlti.TurnitinLtiViewReportCommand
import javax.validation.Valid
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation
import org.springframework.http.HttpStatus
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiResponse

/**
 * Provides access to the Turnitin Document Viewer for a submission
 * that's been submitted to Turnitin.
 */
@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/turnitin-lti-report/{attachment}"))
class TurnitinLtiReportController extends CourseworkController {

	@ModelAttribute("turnitinLtiViewReportCommand")
	def command(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("attachment") attachment: FileAttachment,
		user: CurrentUser
	) = {
		TurnitinLtiViewReportCommand(module, assignment, attachment, user)
	}

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def goToReport(@Valid @ModelAttribute("turnitinLtiViewReportCommand") command: Appliable[TurnitinLtiResponse], errors: Errors): Mav = {
		val response = command.apply()
		if (!response.success && response.responseCode.isDefined && response.responseCode.get != HttpStatus.OK.value) {
			Mav("admin/assignments/turnitinlti/report_error", "problem" -> s"unexpected-response-code")
		}	else {
				if (response.redirectUrl.isDefined) Mav(s"redirect:${response.redirectUrl.get}")
				else Mav("admin/assignments/turnitinlti/report_error", "problem" -> "no-object")
		}
	}

}