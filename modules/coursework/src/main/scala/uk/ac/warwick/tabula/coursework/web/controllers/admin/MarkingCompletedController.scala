package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.MarkingCompletedCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.actions.UploadMarkerFeedback
import javax.validation.Valid
import uk.ac.warwick.tabula.data.Transactions._

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/marking-completed"))
class MarkingCompletedController extends CourseworkController {

	@ModelAttribute
	def command(@PathVariable("assignment") assignment: Assignment, user: CurrentUser) =
		new MarkingCompletedCommand(assignment, user, assignment.isFirstMarker(user.apparentUser))

	validatesSelf[MarkingCompletedCommand]

	def confirmView(assignment: Assignment) = Mav("admin/assignments/markerfeedback/marking-complete",
		"assignment" -> assignment)

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.markerFeedback(assignment))

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment) = RedirectBack(assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment,
		            form: MarkingCompletedCommand, errors: Errors) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(UploadMarkerFeedback(assignment))
		form.onBind()
		form.preSubmitValidation()
		confirmView(assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment,
		         @Valid form: MarkingCompletedCommand, errors: Errors) = {
		transactional() {
			mustBeLinked(assignment, module)
			mustBeAbleTo(UploadMarkerFeedback(assignment))
			form.onBind()
			if (errors.hasErrors)
				showForm(module,assignment, form, errors)
			else {
				form.preSubmitValidation()
				form.apply()
				RedirectBack(assignment)
			}
		}
	}

}
