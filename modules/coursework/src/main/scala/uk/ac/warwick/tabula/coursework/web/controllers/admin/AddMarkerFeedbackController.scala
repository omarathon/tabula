package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.AddMarkerFeedbackCommand
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/feedback"))
class AddMarkerFeedbackController extends CourseworkController {

	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) =
		new AddMarkerFeedbackCommand(module, assignment, user, true) //TODO switch flag depending on which marker this is

	@RequestMapping(method = Array(HEAD, GET))
	def uploadForm(@ModelAttribute cmd: AddMarkerFeedbackCommand): Mav = {
		Mav("admin/assignments/markerfeedback/form")
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmUpload(@ModelAttribute cmd: AddMarkerFeedbackCommand, errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadForm(cmd)
		} else {
			cmd.postExtractValidation(errors)
			cmd.processStudents()
			Mav("admin/assignments/markerfeedback/preview")
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@ModelAttribute cmd: AddMarkerFeedbackCommand, errors: Errors): Mav = {

		cmd.preExtractValidation(errors)
		cmd.postExtractValidation(errors)

		if (errors.hasErrors) {
			confirmUpload(cmd, errors)
		} else {
			// do apply, redirect back
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
	}

}
