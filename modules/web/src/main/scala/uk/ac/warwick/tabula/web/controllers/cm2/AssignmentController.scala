package uk.ac.warwick.tabula.web.controllers.cm2

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.StudentSubmissionAndFeedbackCommand.StudentSubmissionInformation
import uk.ac.warwick.tabula.commands.cm2.{CurrentUserSubmissionAndFeedbackCommandState, StudentSubmissionAndFeedbackCommand}
import uk.ac.warwick.tabula.commands.cm2.assignments.{SubmitAssignmentCommand, SubmitAssignmentRequest}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent

/**
	* This is the main student-facing and non-student-facing controller for handling esubmission and return of feedback.
	* If the studentMember is not specified it works for the current user, whether they are a member of not.
	*/

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/submission/{assignment}"))
class AssignmentController extends CourseworkController
	with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent with AutowiringFeaturesComponent {

	type StudentSubmissionAndFeedbackCommand = Appliable[StudentSubmissionInformation] with CurrentUserSubmissionAndFeedbackCommandState
	type SubmitAssignmentCommand = Appliable[Submission] with SubmitAssignmentRequest

	hideDeletedItems

	validatesSelf[SelfValidating]

	@ModelAttribute("submitAssignmentCommand")
	def formOrNull(@PathVariable assignment: Assignment, user: CurrentUser): SubmitAssignmentCommand = {
		restricted(SubmitAssignmentCommand.self(mandatory(assignment.module), mandatory(assignment), user)).orNull
	}

	@ModelAttribute("studentSubmissionAndFeedbackCommand")
	def studentSubmissionAndFeedbackCommand(@PathVariable assignment: Assignment, user: CurrentUser) =
		StudentSubmissionAndFeedbackCommand(assignment.module, assignment, user)

	@ModelAttribute("willCheckpointBeCreated")
	def willCheckpointBeCreated(
		@PathVariable assignment: Assignment,
		user: CurrentUser
	) = {
		val submission = new Submission(user.universityId)
		submission.assignment = assignment
		submission.submittedDate = DateTime.now
		submission.userId = user.userId
		attendanceMonitoringCourseworkSubmissionService.getCheckpoints(submission).nonEmpty
	}

	/**
		* Sitebuilder-embeddable view.
		*/
	@RequestMapping(method = Array(HEAD, GET), params = Array("embedded"))
	def embeddedView(
		@ModelAttribute("studentSubmissionAndFeedbackCommand") infoCommand: StudentSubmissionAndFeedbackCommand,
		@ModelAttribute("submitAssignmentCommand") formOrNull: SubmitAssignmentCommand,
		errors: Errors
	) = {
		view(infoCommand, formOrNull, errors).embedded
	}

	@RequestMapping(method = Array(HEAD, GET), params = Array("!embedded"))
	def view(
		@ModelAttribute("studentSubmissionAndFeedbackCommand") infoCommand: StudentSubmissionAndFeedbackCommand,
		@ModelAttribute("submitAssignmentCommand") formOrNull: SubmitAssignmentCommand,
		errors: Errors
	) = {
		val form = Option(formOrNull)
		val info = infoCommand.apply()

		// If the user has feedback but doesn't have permission to submit, form will be null here, so we can't just get module/assignment from that
		Mav(
			s"$urlPrefix/submit/assignment",
			"errors" -> errors,
			"feedback" -> info.feedback,
			"submission" -> info.submission,
			"justSubmitted" -> form.exists { _.justSubmitted },
			"canSubmit" -> info.canSubmit,
			"canReSubmit" -> info.canReSubmit,
			"hasExtension" -> info.extension.isDefined,
			"hasActiveExtension" -> info.extension.exists(_.approved), // active = has been approved
			"extension" -> info.extension,
			"isExtended" -> info.isExtended,
			"extensionRequested" -> info.extensionRequested,
			"hasDisability" -> info.hasDisability,
			"isSelf" -> true)
			.withTitle(infoCommand.module.name + " (" + infoCommand.module.code.toUpperCase + ")" + " - " + infoCommand.assignment.name)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@ModelAttribute("studentSubmissionAndFeedbackCommand") infoCommand: StudentSubmissionAndFeedbackCommand,
		@Valid @ModelAttribute("submitAssignmentCommand") form: SubmitAssignmentCommand,
		errors: Errors
	) = {
		// We know form isn't null here because of permissions checks on the info command
		if (errors.hasErrors) {
			view(infoCommand, form, errors)
		} else {
			transactional() { form.apply() }

			Redirect(Routes.assignment(form.assignment)).addObjects("justSubmitted" -> true)
		}
	}

}
