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
	def goToReport(@Valid @ModelAttribute("turnitinLtiViewReportCommand") command: Appliable[Mav], errors: Errors): Mav = command.apply()

}