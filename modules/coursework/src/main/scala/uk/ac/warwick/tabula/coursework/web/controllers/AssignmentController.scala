package uk.ac.warwick.tabula.coursework.web.controllers

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.actions.Submit
import uk.ac.warwick.tabula.coursework.commands.assignments.SendSubmissionReceiptCommand
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmitAssignmentCommand
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.data.model.Module
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.data.model.Feedback

/** This is the main student-facing controller for handling esubmission and return of feedback.
 *
 */
@Controller
@RequestMapping(Array("/module/{module}/{assignment}"))
class AssignmentController extends AbstractAssignmentController {

	hideDeletedItems

	validatesSelf[SubmitAssignmentCommand]

	@ModelAttribute def form(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) = {
		val cmd = new SubmitAssignmentCommand(mandatory(assignment), user)
		cmd.module = module
		cmd
	}

	/**
	 * Sitebuilder-embeddable view.
	 */
	@RequestMapping(method = Array(HEAD, GET), params = Array("embedded"))
	def embeddedView(user: CurrentUser, form: SubmitAssignmentCommand, errors: Errors) = {
		view(user, form, errors).embedded
	}

	@RequestMapping(method = Array(HEAD, GET), params = Array("!embedded"))
	def view(user: CurrentUser, form: SubmitAssignmentCommand, errors: Errors) = {
		val assignment = form.assignment
		val module = form.module

		if (!user.loggedIn) {
			RedirectToSignin()
		} else {
		    val feedback = checkCanGetFeedback(assignment, user)

			form.onBind
			checks(form, feedback)

			val submission = assignmentService.getSubmissionByUniId(assignment, user.universityId).filter { _.submitted }

			val extension = assignment.extensions.find(_.userId == user.apparentId)
			val isExtended = assignment.isWithinExtension(user.apparentId)
			val extensionRequested = extension.isDefined && !extension.get.isManual

			val canSubmit = assignment.submittable(user.apparentId)
			val canReSubmit = assignment.resubmittable(user.apparentId)

			/*
			 * Submission values are an unordered set without any proper name, so
			 * match them up into an ordered sequence of pairs.
			 * 
			 * If a submission value is missing, the right hand is None.
			 * If any submission value doesn't match the assignment fields, it just isn't shown.
			 */		
			val submissionValues = submission.map { submission =>
				for (field <- assignment.fields) yield ( field -> submission.getValue(field) )
			}.getOrElse(Seq.empty)

			Mav(
				"submit/assignment",
				"module" -> module,
				"assignment" -> assignment,
				"feedback" -> feedback,
				"submission" -> submission,
				"justSubmitted" -> form.justSubmitted,
				"canSubmit" -> canSubmit,
				"canReSubmit" -> canReSubmit,
				"extension" -> extension,
				"isExtended" -> isExtended,
				"extensionRequested" -> extensionRequested)

		}
	}

	@RequestMapping(method = Array(POST))
	def submit(@PathVariable module: Module, user: CurrentUser, @Valid form: SubmitAssignmentCommand, errors: Errors) = {
		transactional() {
			val assignment = form.assignment
			val module = form.module
			form.onBind
			checks(form, None)
			if (errors.hasErrors || !user.loggedIn) {
				view(user, form, errors)
			} else {
				val submission = form.apply
				val sendReceipt = new SendSubmissionReceiptCommand(submission, user)
				sendReceipt.apply()
				Redirect(Routes.assignment(form.assignment)).addObjects("justSubmitted" -> true)
			}
		}
	}

	private def checks(form: SubmitAssignmentCommand, feedback: Option[Feedback]) = {
		val assignment = form.assignment
		val module = form.module
		mustBeLinked(mandatory(assignment), mandatory(module))
		if (feedback.isEmpty && !can(Submit(assignment))) { // includes check for restricted submission.
			throw new SubmitPermissionDeniedException(assignment)
		}
	}

	private def hasPermission(form: SubmitAssignmentCommand) = can(Submit(form.assignment))

}
