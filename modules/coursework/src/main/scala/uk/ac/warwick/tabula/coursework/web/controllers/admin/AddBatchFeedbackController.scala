package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.AddFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.actions.Participate
import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/batch"))
class AddBatchFeedbackController extends CourseworkController {
	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) =
		new AddFeedbackCommand(module, assignment, user)

	// Add the common breadcrumbs to the model.
	def crumbed(mav: Mav, module: Module) = mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	@RequestMapping(method = Array(HEAD, GET))
	def uploadZipForm(@ModelAttribute cmd: AddFeedbackCommand): Mav = {
		crumbed(Mav("admin/assignments/feedback/zipform"), cmd.module)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@ModelAttribute cmd: AddFeedbackCommand, errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadZipForm(cmd)
		} else {
			cmd.postExtractValidation(errors)
			crumbed(Mav("admin/assignments/feedback/zipreview"), cmd.module)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@ModelAttribute cmd: AddFeedbackCommand, errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		cmd.postExtractValidation(errors)
		if (errors.hasErrors) {
			crumbed(Mav("admin/assignments/feedback/zipreview"), cmd.module)
		} else {
			// do apply, redirect back
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
	}
}