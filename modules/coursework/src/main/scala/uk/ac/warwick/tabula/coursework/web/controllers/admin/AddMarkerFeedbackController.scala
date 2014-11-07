package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.coursework.commands.assignments.AddMarkerFeedbackCommand
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback"))
class AddMarkerFeedbackController extends CourseworkController {

	@ModelAttribute def command(@PathVariable("module") module: Module,
															@PathVariable("assignment") assignment: Assignment,
															@PathVariable("marker") marker: User,
															user: CurrentUser) =
		new AddMarkerFeedbackCommand(module, assignment, marker, user)

	@RequestMapping(method = Array(HEAD, GET))
	def uploadForm(@PathVariable("module") module: Module,
								 @PathVariable("assignment") assignment: Assignment,
								 @PathVariable("marker") marker: User,
								 @ModelAttribute cmd: AddMarkerFeedbackCommand): Mav = {
		Mav("admin/assignments/markerfeedback/form").crumbs(
			Breadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, marker)), "")
		)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmUpload(@PathVariable("module") module: Module,
										@PathVariable("assignment") assignment: Assignment,
										@PathVariable("marker") marker: User,
										@ModelAttribute cmd: AddMarkerFeedbackCommand,
										errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadForm(module, assignment, marker, cmd)
		} else {
			cmd.postExtractValidation(errors)
			cmd.processStudents()
			Mav("admin/assignments/markerfeedback/preview").crumbs(
				Breadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, marker)), "")
			)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable("module") module: Module,
							 @PathVariable("assignment") assignment: Assignment,
							 @PathVariable("marker") marker: User,
							 @ModelAttribute cmd: AddMarkerFeedbackCommand,
							 errors: Errors): Mav = {

		cmd.preExtractValidation(errors)
		cmd.postExtractValidation(errors)

		if (errors.hasErrors) {
			confirmUpload(module, assignment, marker, cmd, errors)
		} else {
			// do apply, redirect back
			cmd.apply()
			Redirect(Routes.admin.assignment.markerFeedback(assignment, marker))
		}
	}

}
