package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Module, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.validation.Errors
import javax.validation.Valid
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.coursework.commands.assignments.MarkingCompletedCommand
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConversions._

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/marking-completed"))
class MarkingCompletedController extends CourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("markingCompletedCommand")
	def command(@PathVariable("module") module: Module,
							@PathVariable("assignment") assignment: Assignment,
							@PathVariable("marker") marker: User,
							submitter: CurrentUser) =
		MarkingCompletedCommand(module, assignment, marker, submitter)


	def RedirectBack(assignment: Assignment, command: MarkingCompletedCommand) = {
		if(command.onlineMarking){
			Redirect(Routes.admin.assignment.onlineMarkerFeedback(assignment, command.user))
		} else {
			Redirect(Routes.admin.assignment.markerFeedback(assignment, command.user))
		}
	}

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment, form: MarkingCompletedCommand) = RedirectBack(assignment, form)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("marker") marker: User,
		@ModelAttribute("markingCompletedCommand") form: MarkingCompletedCommand,
		errors: Errors
	) = {
		form.preSubmitValidation()

		val isUserALaterMarker = form.pendingMarkerFeedbacks.exists{ markerFeedback =>
			def checkNextMarkerFeedbackForMarker(thisMarkerFeedback: MarkerFeedback): Boolean = {
				form.nextMarkerFeedback(thisMarkerFeedback).exists{ mf =>
					if (mf.getMarkerUsercode.getOrElse("") == user.apparentId)
						true
					else
						checkNextMarkerFeedbackForMarker(mf)
				}
			}
			checkNextMarkerFeedbackForMarker(markerFeedback)
		}

		Mav("admin/assignments/markerfeedback/marking-complete",
			"assignment" -> assignment,
			"onlineMarking" -> form.onlineMarking,
			"marker" -> form.user,
			"isUserALaterMarker" -> isUserALaterMarker
		).crumbs(
			Breadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, marker)), "")
		)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("marker") marker: User,
		@Valid @ModelAttribute("markingCompletedCommand") form: MarkingCompletedCommand,
		errors: Errors
	) = {
		transactional() {
			if (errors.hasErrors)
				showForm(module,assignment, marker, form, errors)
			else {
				form.preSubmitValidation()
				form.apply()
				RedirectBack(assignment, form)
			}
		}
	}

}
