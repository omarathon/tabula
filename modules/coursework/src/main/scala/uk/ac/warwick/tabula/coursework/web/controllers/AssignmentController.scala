package uk.ac.warwick.tabula.coursework.web.controllers

import scala.collection.JavaConversions._
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.commands.assignments.SendSubmissionReceiptCommand
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmitAssignmentCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.SubmitPermissionDeniedException
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.coursework.commands.assignments.SendSubmissionNotifyCommand
import uk.ac.warwick.tabula.services.SubmissionService

/** This is the main student-facing controller for handling esubmission and return of feedback.
 *
 */
@Controller
@RequestMapping(Array("/module/{module}/{assignment}"))
class AssignmentController extends CourseworkController {
	
	var submissionService = Wire[SubmissionService]
	var feedbackDao = Wire[FeedbackDao]

	hideDeletedItems

	validatesSelf[SubmitAssignmentCommand]
	
	private def getFeedback(assignment: Assignment, user: CurrentUser) = 
		feedbackDao.getFeedbackByUniId(assignment, user.universityId).filter(_.released)

	@ModelAttribute def formOrNull(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) = 
		restrictedBy {
			getFeedback(assignment, user).isDefined
		} (new SubmitAssignmentCommand(mandatory(module), mandatory(assignment), user)).orNull

	/**
	 * Sitebuilder-embeddable view.
	 */
	@RequestMapping(method = Array(HEAD, GET), params = Array("embedded"))
	def embeddedView(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser, formOrNull: SubmitAssignmentCommand, errors: Errors) = {
		view(module, assignment, user, formOrNull, errors).embedded
	}

	@RequestMapping(method = Array(HEAD, GET), params = Array("!embedded"))
	def view(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser, formOrNull: SubmitAssignmentCommand, errors: Errors) = {
		val form = Option(formOrNull)
		
		// If the user has feedback but doesn't have permission to submit, form will be null here, so we can't just get module/assignment from that
		if (!user.loggedIn) {
			RedirectToSignin()
		} else {
		    val feedback = getFeedback(assignment, user)

			val submission = submissionService.getSubmissionByUniId(assignment, user.universityId).filter { _.submitted }

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
				"justSubmitted" -> (form map { _.justSubmitted } getOrElse (false)),
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
	def submit(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser, @Valid formOrNull: SubmitAssignmentCommand, errors: Errors) = {
		val form: SubmitAssignmentCommand = Option(formOrNull) getOrElse {
			throw new SubmitPermissionDeniedException(assignment)
		}
				
		/*
		 * Note the multiple transactions. The submission transaction should commit before the confirmation email
		 * command runs, to ensure that it has fully committed successfully. So don't wrap this method in an outer transaction
		 * or you'll just make it be one transaction! (HFC-385)
		 */
		if (errors.hasErrors || !user.loggedIn) {
			view(form.module, form.assignment, user, form, errors)
		} else {
			val submission = transactional() { form.apply() }
			// submission creation should be committed to DB at this point, 
			// so we can safely send out a submission receipt.
			transactional() {
				val sendReceipt = new SendSubmissionReceiptCommand(module, assignment, submission, user)
				sendReceipt.apply()
			}
			
			transactional() {
 				val moduleManagers = submission.assignment.module.managers
 				val notifyModuleManager = new SendSubmissionNotifyCommand(submission, moduleManagers)
 				notifyModuleManager.apply()
 			}
			
			Redirect(Routes.assignment(form.assignment)).addObjects("justSubmitted" -> true)
		}
	}

}
