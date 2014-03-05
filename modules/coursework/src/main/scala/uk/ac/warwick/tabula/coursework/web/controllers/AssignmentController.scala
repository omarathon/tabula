package uk.ac.warwick.tabula.coursework.web.controllers

import scala.collection.JavaConversions._
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.commands.assignments.{ViewOnlineFeedbackCommand, SubmitAssignmentCommand}
import uk.ac.warwick.tabula.data.model.{Submission, Assignment, Module}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.SubmitPermissionDeniedException
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{MonitoringPointProfileTermAssignmentService, SubmissionService, FeedbackService}
import org.joda.time.DateTime

/**
 * This is the main student-facing controller for handling esubmission and return of feedback.
 */
@Controller
@RequestMapping(Array("/module/{module}/{assignment}"))
class AssignmentController extends CourseworkController {

	var submissionService = Wire[SubmissionService]
	var feedbackService = Wire[FeedbackService]
	var monitoringPointProfileTermAssignmentService = Wire[MonitoringPointProfileTermAssignmentService]

	hideDeletedItems

	validatesSelf[SubmitAssignmentCommand]

	private def getFeedback(assignment: Assignment, user: CurrentUser) =
		feedbackService.getFeedbackByUniId(assignment, user.universityId).filter(_.released)

	@ModelAttribute def formOrNull(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) = {
		val cmd = new ViewOnlineFeedbackCommand(assignment, user)
		
		val feedback =
			if (cmd.hasFeedback) cmd.apply() // Log audit event
			else None

		restrictedBy {
			feedback.isDefined
		} (new SubmitAssignmentCommand(mandatory(module), mandatory(assignment), user)).orNull

	}

	@ModelAttribute("willCheckpointBeCreated") def willCheckpointBeCreated(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser) = {
		val submission = new Submission(user.universityId)
		submission.assignment = assignment
		submission.submittedDate = DateTime.now
		submission.userId = user.userId
		!monitoringPointProfileTermAssignmentService.getCheckpointsForSubmission(submission).isEmpty
	}

	/**
	 * Sitebuilder-embeddable view.
	 */
	@RequestMapping(method = Array(HEAD, GET), params = Array("embedded"))
	def embeddedView(
			@PathVariable("module") module: Module,
			@PathVariable("assignment") assignment: Assignment,
			user: CurrentUser,
			formOrNull: SubmitAssignmentCommand,
			errors: Errors) = {
		view(module, assignment, user, formOrNull, errors).embedded
	}

	@RequestMapping(method = Array(HEAD, GET), params = Array("!embedded"))
	def view(
			@PathVariable("module") module: Module,
			@PathVariable("assignment") assignment: Assignment,
			user: CurrentUser,
			formOrNull: SubmitAssignmentCommand,
			errors: Errors) = {
		val form = Option(formOrNull)

		// If the user has feedback but doesn't have permission to submit, form will be null here, so we can't just get module/assignment from that
		if (!user.loggedIn) {
			RedirectToSignin()
		} else {
		    val feedback = getFeedback(assignment, user)

			val submission = submissionService.getSubmissionByUniId(assignment, user.universityId).filter { _.submitted }

			val extension = assignment.extensions.find(_.isForUser(user.apparentUser))
			val isExtended = assignment.isWithinExtension(user.apparentUser)
			val extensionRequested = extension.isDefined && !extension.get.isManual

			val canSubmit = assignment.submittable(user.apparentUser)
			val canReSubmit = assignment.resubmittable(user.apparentUser)

			Mav(
				"submit/assignment",
				"module" -> module,
				"assignment" -> assignment,
				"feedback" -> feedback,
				"submission" -> submission,
				"justSubmitted" -> ( form exists { _.justSubmitted } ),
				"canSubmit" -> canSubmit,
				"canReSubmit" -> canReSubmit,
				"hasExtension" -> extension.isDefined,
				"extension" -> extension,
				"isExtended" -> isExtended,
				"extensionRequested" -> extensionRequested)
				.withTitle(module.name + " (" + module.code.toUpperCase + ")" + " - " + assignment.name)

		}
	}

	@RequestMapping(method = Array(POST))
	def submit(
			@PathVariable("module") module: Module,
			@PathVariable("assignment") assignment: Assignment,
			user: CurrentUser,
			@Valid formOrNull: SubmitAssignmentCommand,
			errors: Errors) = {
		val form: SubmitAssignmentCommand = Option(formOrNull) getOrElse {
			throw new SubmitPermissionDeniedException(assignment)
		}

		if (errors.hasErrors || !user.loggedIn) {
			view(form.module, form.assignment, user, form, errors)
		} else {
			transactional() { form.apply() }

			Redirect(Routes.assignment(form.assignment)).addObjects("justSubmitted" -> true)
		}
	}

}
