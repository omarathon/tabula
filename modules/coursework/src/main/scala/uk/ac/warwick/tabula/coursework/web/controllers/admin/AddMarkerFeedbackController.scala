package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.AddMarkerFeedbackCommand
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.actions.UploadMarkerFeedback
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/feedback"))
class AddMarkerFeedbackController extends CourseworkController {

	@ModelAttribute def command(@PathVariable assignment: Assignment, user: CurrentUser) =
		new AddMarkerFeedbackCommand(assignment, user, true) //TODO switch flag depending on which marker this is

	def onBind(cmd: AddMarkerFeedbackCommand){ cmd.onBind }

	@RequestMapping(method = Array(HEAD, GET))
	def uploadForm(@PathVariable module: Module, @PathVariable assignment: Assignment,
	                  @ModelAttribute cmd: AddMarkerFeedbackCommand): Mav = {
		doChecks(module, assignment)
		Mav("admin/assignments/markerfeedback/form")
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmUpload(@PathVariable module: Module, @PathVariable assignment: Assignment,
	                       @ModelAttribute cmd: AddMarkerFeedbackCommand, errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadForm(module, assignment, cmd)
		} else {
			cmd.onBind
			cmd.postExtractValidation(errors)
			cmd.processStudents()
			doChecks(module, assignment)
			Mav("admin/assignments/markerfeedback/preview")
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable module: Module, @PathVariable assignment: Assignment,
							 @ModelAttribute cmd: AddMarkerFeedbackCommand, errors: Errors): Mav = {

		doChecks(module, assignment)
		cmd.preExtractValidation(errors)
		cmd.onBind
		cmd.postExtractValidation(errors)

		if (errors.hasErrors) {
			confirmUpload(module, assignment, cmd, errors)
		} else {
			// do apply, redirect back
			cmd.apply()
			Redirect(Routes.admin.module(module))
		}
	}

	def doChecks(module: Module, assignment: Assignment){
		mustBeAbleTo(UploadMarkerFeedback(assignment))
		mustBeLinked(mandatory(assignment), mandatory(module))
	}

}
