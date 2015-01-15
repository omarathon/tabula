package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.validation.Errors
import javax.validation.Valid
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.coursework.commands.assignments.{MarkingUncompletedState, MarkingUncompletedCommand}
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/marking-uncompleted"))
class MarkingUncompletedController extends CourseworkController {

	type MarkingUncompletedCommand = Appliable[Unit] with MarkingUncompletedState

	validatesSelf[SelfValidating]

	@ModelAttribute("markingUncompletedCommand")
	def command(@PathVariable("module") module: Module,
							@PathVariable("assignment") assignment: Assignment,
							@PathVariable("marker") marker: User,
							submitter: CurrentUser) =
		MarkingUncompletedCommand(module, assignment, marker, submitter)

	def RedirectBack(assignment: Assignment, command: MarkingUncompletedCommand) = {
			Redirect(Routes.admin.assignment.markerFeedback(assignment, command.user))
	}

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment, @ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand) = RedirectBack(assignment, form)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("marker") marker: User,
		@ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand,
		errors: Errors
	) = {

		val previousStageRole = requestInfo
			.flatMap(_.requestParameters.get("previousStageRole"))
			.flatMap(_.headOption)

		Mav("admin/assignments/markerfeedback/marking-uncomplete",
			"assignment" -> assignment,
			"formAction" -> Routes.admin.assignment.markerFeedback.uncomplete(assignment, marker, previousStageRole.getOrElse("Marker")),
			"marker" -> form.user,
			"previousStageRole" -> previousStageRole
		).crumbs(
			Breadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, marker)), "")
		)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("marker") marker: User,
		@Valid @ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand,
		errors: Errors
	) = transactional() {
			if (errors.hasErrors)
				showForm(module,assignment, marker, form, errors)
			else {
				form.apply()
				RedirectBack(assignment, form)
			}
		}

}


// Redirects users trying to access a marking workflow using the old style URL
@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/marking-uncompleted"))
class MarkingUncompletedControllerCurrentUser extends CourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser) = {
		Redirect(Routes.admin.assignment.markerFeedback.uncomplete(assignment, currentUser.apparentUser))
	}

}