package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.cm2.assignments.DeleteSubmissionsAndFeedbackCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, Submission}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

/**
	* Takes a list of student university IDs and deletes either all their submissions, or all their feedback, or both,
	* depending on the value of submissionOrFeedback
	*/
object DeleteSubmissionsAndFeedbackCommand {
	case class Result(submissions: Seq[Submission], feedbacks: Seq[Feedback])
	type Command = Appliable[Result] with SelfValidating

	val SubmissionOnly = "submissionOnly"
	val FeedbackOnly = "feedbackOnly"
	val SubmissionAndFeedback = "submissionAndFeedback"

	def apply(assignment: Assignment): Command =
		new DeleteSubmissionsAndFeedbackCommandInternal(assignment)
			with ComposableCommand[Result]
			with DeleteSubmissionsAndFeedbackPermissions
			with DeleteSubmissionsAndFeedbackValidation
			with DeleteSubmissionsAndFeedbackDescription
			with AutowiringSubmissionServiceComponent
			with AutowiringFeedbackServiceComponent
			with AutowiringZipServiceComponent

}

trait DeleteSubmissionsAndFeedbackState extends SelectedStudentsState {
	def assignment: Assignment
}

trait DeleteSubmissionsAndFeedbackRequest extends DeleteSubmissionsAndFeedbackState with SelectedStudentsRequest {
	var submissionOrFeedback: String = ""
	var confirm: Boolean = false

	def shouldDeleteSubmissions: Boolean = submissionOrFeedback == SubmissionAndFeedback || submissionOrFeedback == SubmissionOnly
	def shouldDeleteFeedback: Boolean = submissionOrFeedback == SubmissionAndFeedback || submissionOrFeedback == FeedbackOnly
}

class DeleteSubmissionsAndFeedbackCommandInternal(val assignment: Assignment)
	extends CommandInternal[Result] with DeleteSubmissionsAndFeedbackRequest {
	self: SubmissionServiceComponent
		with FeedbackServiceComponent
		with ZipServiceComponent =>

	override def applyInternal(): Result = transactional() {
		val deletedSubmissions =
			if (shouldDeleteSubmissions)
				submissions.map { sub =>
					submissionService.delete(sub)
					sub
				}
			else Nil

		if (deletedSubmissions.nonEmpty)
			zipService.invalidateSubmissionZip(assignment)

		val deletedFeedback =
			if (shouldDeleteFeedback)
				feedbacks.map { f =>
					feedbackService.delete(f)
					zipService.invalidateIndividualFeedbackZip(f)
					f
				}
			else Nil

		Result(deletedSubmissions, deletedFeedback)
	}

}

trait DeleteSubmissionsAndFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DeleteSubmissionsAndFeedbackState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.AssignmentFeedback.Manage, mandatory(assignment))
		p.PermissionCheck(Permissions.Submission.Delete, mandatory(assignment))
	}

}

trait DeleteSubmissionsAndFeedbackValidation extends SelfValidating {
	self: DeleteSubmissionsAndFeedbackRequest =>

	override def validate(errors: Errors): Unit = {
		if (!Seq(SubmissionOnly, FeedbackOnly, SubmissionAndFeedback).contains(submissionOrFeedback)) {
			errors.rejectValue("submissionOrFeedback", "invalid")
		}

		if (!confirm) errors.rejectValue("confirm", "submissionOrFeedback.delete.confirm")
	}
}

trait DeleteSubmissionsAndFeedbackDescription extends Describable[Result] {
	self: DeleteSubmissionsAndFeedbackRequest =>

	override lazy val eventName: String = "DeleteSubmissionsAndFeedback"

	override def describe(d: Description): Unit =
		d.assignment(assignment)
		 .studentUsercodes(students.asScala)

	override def describeResult(d: Description, result: Result): Unit =
		d.submissions(result.submissions)
		 .feedbacks(result.feedbacks)
		 .fileAttachments(result.submissions.flatMap(_.allAttachments) ++ result.feedbacks.flatMap(_.attachments.asScala))
}