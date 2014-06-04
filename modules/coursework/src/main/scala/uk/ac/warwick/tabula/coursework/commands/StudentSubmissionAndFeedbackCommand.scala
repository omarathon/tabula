package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.data.model.{Submission, Feedback, Member, Assignment, Module}
import uk.ac.warwick.tabula.commands.{Describable, Description, CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.CurrentUser
import StudentSubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
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

	def apply(module: Module, assignment: Assignment, member: Member, viewingUser: CurrentUser) =
		new StudentSubmissionAndFeedbackCommandInternal(module, assignment)
			with StudentMemberSubmissionAndFeedbackCommandState
			with StudentMemberSubmissionAndFeedbackCommandPermissions
			with AutowiringFeedbackServiceComponent
			with AutowiringSubmissionServiceComponent
			with ComposableCommand[StudentSubmissionInformation]
			with Unaudited with ReadOnly {
			def studentMember = member
			def currentUser = viewingUser
		}

	def apply(module: Module, assignment: Assignment, user: CurrentUser) =
		new StudentSubmissionAndFeedbackCommandInternal(module, assignment)
			with CurrentUserSubmissionAndFeedbackCommandState
			with CurrentUserSubmissionAndFeedbackCommandPermissions
			with AutowiringFeedbackServiceComponent
			with AutowiringSubmissionServiceComponent
			with ComposableCommand[StudentSubmissionInformation]
			with Unaudited with ReadOnly {
			def currentUser = user
		}
}

trait StudentSubmissionAndFeedbackCommandState {
	self: FeedbackServiceComponent with SubmissionServiceComponent =>

	def module: Module
	def assignment: Assignment
	def studentUser: User
	def viewer: User

	def feedback = feedbackService.getFeedbackByUniId(assignment, studentUser.getWarwickId).filter(_.released)
	def submission = submissionService.getSubmissionByUniId(assignment, studentUser.getWarwickId).filter { _.submitted }
}

trait StudentMemberSubmissionAndFeedbackCommandState extends StudentSubmissionAndFeedbackCommandState {
	self: FeedbackServiceComponent with SubmissionServiceComponent =>

	def studentMember: Member
	def currentUser: CurrentUser

	final def studentUser = studentMember.asSsoUser
	final def viewer = currentUser.apparentUser
}

trait CurrentUserSubmissionAndFeedbackCommandState extends StudentSubmissionAndFeedbackCommandState {
	self: FeedbackServiceComponent with SubmissionServiceComponent =>

	def currentUser: CurrentUser

	final def studentUser = currentUser.apparentUser
	final def viewer = currentUser.apparentUser
}

abstract class StudentSubmissionAndFeedbackCommandInternal(val module: Module, val assignment: Assignment)
	extends CommandInternal[StudentSubmissionInformation] with StudentSubmissionAndFeedbackCommandState {
	self: FeedbackServiceComponent with SubmissionServiceComponent =>

	def applyInternal() = {
		val extension = assignment.extensions.asScala.find(_.isForUser(studentUser))

		// Log a ViewOnlineFeedback event if the student itself is viewing
		feedback.filter { _.universityId == viewer.getWarwickId }.foreach { feedback =>
			ViewOnlineFeedbackCommand(feedback).apply()
		}

		StudentSubmissionInformation(
			submission = submission,
			feedback = feedback,
			extension = extension,

			isExtended = assignment.isWithinExtension(studentUser),
			extensionRequested = extension.isDefined && !extension.get.isManual,

			canSubmit = assignment.submittable(studentUser),
			canReSubmit = assignment.resubmittable(studentUser)
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

trait CurrentUserSubmissionAndFeedbackCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CurrentUserSubmissionAndFeedbackCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(mandatory(assignment), mandatory(module))

		var perms = collection.mutable.MutableList[CheckablePermission]()

		submission.foreach { submission => perms += CheckablePermission(Permissions.Submission.Read, Some(submission)) }
		feedback.foreach { feedback => perms += CheckablePermission(Permissions.Feedback.Read, Some(feedback)) }

		perms += CheckablePermission(Permissions.Submission.Create, Some(assignment))

		p.PermissionCheckAny(perms)
	}
}

object ViewOnlineFeedbackCommand {
	def apply(feedback: Feedback) =
		new ViewOnlineFeedbackCommandInternal(feedback)
			with ComposableCommand[Feedback]
			with ViewOnlineFeedbackCommandDescription
			with ViewOnlineFeedbackCommandPermissions
}

trait ViewOnlineFeedbackCommandState {
	def feedback: Feedback
}

class ViewOnlineFeedbackCommandInternal(val feedback: Feedback) extends CommandInternal[Feedback] with ViewOnlineFeedbackCommandState {
	def applyInternal() = feedback
}

trait ViewOnlineFeedbackCommandDescription extends Describable[Feedback] {
	self: ViewOnlineFeedbackCommandState =>

	override lazy val eventName = "ViewOnlineFeedback"

	def describe(d: Description) = d.assignment(feedback.assignment).properties("student" -> feedback.universityId)
}

trait ViewOnlineFeedbackCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewOnlineFeedbackCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.Read, mandatory(feedback))
	}
}