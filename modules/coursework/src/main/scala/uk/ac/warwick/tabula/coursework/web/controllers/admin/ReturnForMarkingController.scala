package uk.ac.warwick.tabula.coursework.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.coursework.commands.assignments.{AdminMarkingUncompletedCommand, MarkingUncompletedState}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/return-submissions"))
class ReturnForMarkingController extends CourseworkController {

	type MarkingUncompletedCommand = Appliable[Unit] with MarkingUncompletedState

	validatesSelf[SelfValidating]

	@ModelAttribute("markingUncompletedCommand")
	def command(@PathVariable("module") module: Module,
							@PathVariable("assignment") assignment: Assignment,
							submitter: CurrentUser) =
		AdminMarkingUncompletedCommand(module, assignment, submitter.apparentUser, submitter)

	def redirectBack(assignment: Assignment, command: MarkingUncompletedCommand) = {
		Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
	}

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment, @ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand) = redirectBack(assignment, form)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
								@PathVariable("module") module: Module,
								@PathVariable("assignment") assignment: Assignment,
								@ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand,
								errors: Errors
								) = {

		Mav("admin/assignments/markerfeedback/marking-uncomplete",
			"assignment" -> assignment,
			"formAction" -> Routes.admin.assignment.markerFeedback.returnsubmissions(assignment),
			"marker" -> form.user,
			"previousStageRole" -> "Marker"
		).crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
							@PathVariable("module") module: Module,
							@PathVariable("assignment") assignment: Assignment,
							@Valid @ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand,
							errors: Errors
							) = transactional() {
		if (errors.hasErrors)
			showForm(module,assignment, form, errors)
		else {
			form.apply()
			redirectBack(assignment, form)
		}
	}

}