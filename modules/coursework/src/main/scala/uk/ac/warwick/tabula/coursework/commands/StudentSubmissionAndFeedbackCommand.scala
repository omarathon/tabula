package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.data.model.{Submission, Feedback, Member, Assignment, Module}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.CurrentUser
import StudentSubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSubmissionServiceComponent, SubmissionServiceComponent, AutowiringFeedbackServiceComponent, FeedbackServiceComponent}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.forms.Extension

object StudentSubmissionAndFeedbackCommand {
	case class StudentSubmissionInformation(
		submission: Option[Submission],
		feedback: Option[Feedback],
		extension: Option[Extension],
		isExtended: Boolean,
		extensionRequested: Boolean,
		canSubmit: Boolean,
		canReSubmit: Boolean
  )

	def apply(module: Module, assignment: Assignment, member: Member) =
		new StudentSubmissionAndFeedbackCommandInternal(module, assignment)
			with StudentMemberSubmissionAndFeedbackCommandState
			with StudentMemberSubmissionAndFeedbackCommandPermissions
			with AutowiringFeedbackServiceComponent
			with AutowiringSubmissionServiceComponent
			with ComposableCommand[StudentSubmissionInformation]
			with Unaudited with ReadOnly {
			def studentMember = member
		}
}

trait StudentSubmissionAndFeedbackCommandState {
	self: FeedbackServiceComponent with SubmissionServiceComponent =>

	def module: Module
	def assignment: Assignment
	def user: User

	def feedback = feedbackService.getFeedbackByUniId(assignment, user.getWarwickId).filter(_.released)
	def submission = submissionService.getSubmissionByUniId(assignment, user.getWarwickId).filter { _.submitted }
}

trait StudentMemberSubmissionAndFeedbackCommandState extends StudentSubmissionAndFeedbackCommandState {
	self: FeedbackServiceComponent with SubmissionServiceComponent =>

	def studentMember: Member

	final def user = studentMember.asSsoUser
}

trait CurrentUserSubmissionAndFeedbackCommandState extends StudentSubmissionAndFeedbackCommandState {
	self: FeedbackServiceComponent with SubmissionServiceComponent =>

	def currentUser: CurrentUser

	final def user = currentUser.apparentUser
}

abstract class StudentSubmissionAndFeedbackCommandInternal(val module: Module, val assignment: Assignment)
	extends CommandInternal[StudentSubmissionInformation] with StudentSubmissionAndFeedbackCommandState {
	self: FeedbackServiceComponent with SubmissionServiceComponent =>

	def applyInternal() = {
		val extension = assignment.extensions.asScala.find(_.isForUser(user))

		StudentSubmissionInformation(
			submission = submission,
			feedback = feedback,
			extension = extension,

			isExtended = assignment.isWithinExtension(user),
			extensionRequested = extension.isDefined && !extension.get.isManual,

			canSubmit = assignment.submittable(user),
			canReSubmit = assignment.resubmittable(user)
		)
	}

}

trait StudentMemberSubmissionAndFeedbackCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: StudentMemberSubmissionAndFeedbackCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(mandatory(assignment), mandatory(module))

		p.PermissionCheck(Permissions.Submission.Read, mandatory(studentMember))
		p.PermissionCheck(Permissions.Feedback.Read, mandatory(studentMember))
	}
}