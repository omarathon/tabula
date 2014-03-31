package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.validation.Errors
import javax.validation.Valid
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.coursework.commands.assignments.MarkingCompletedCommand
import uk.ac.warwick.tabula.commands.SelfValidating

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/marking-completed"))
class MarkingCompletedController extends CourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("markingCompletedCommand")
	def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) =
		MarkingCompletedCommand(module, assignment, user.apparentUser)


	def RedirectBack(assignment: Assignment, command: MarkingCompletedCommand) = {
		if(command.onlineMarking){
			Redirect(Routes.admin.assignment.onlineMarkerFeedback(assignment))
		} else {
			Redirect(Routes.admin.assignment.markerFeedback(assignment))
		}
	}

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment, form: MarkingCompletedCommand) = RedirectBack(assignment, form)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("markingCompletedCommand") form: MarkingCompletedCommand,
		errors: Errors
	) = {
		form.preSubmitValidation()
		Mav("admin/assignments/markerfeedback/marking-complete",
			"assignment" -> assignment,
			"onlineMarking" -> form.onlineMarking)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@Valid @ModelAttribute("markingCompletedCommand") form: MarkingCompletedCommand,
		errors: Errors
	) = {
		transactional() {
			if (errors.hasErrors)
				showForm(module,assignment, form, errors)
			else {
				form.preSubmitValidation()
				form.apply()
				RedirectBack(assignment, form)
			}
		}
	}

}
