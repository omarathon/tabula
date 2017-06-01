package uk.ac.warwick.tabula.commands.cm2.feedback

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.{SelectedStudentsRequest, SelectedStudentsState}
import uk.ac.warwick.tabula.commands.cm2.feedback.PublishFeedbackCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.{FeedbackDueExtensionNotification, FeedbackDueGeneralNotification, FeedbackPublishedNotification, FinaliseFeedbackNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.language.implicitConversions

object PublishFeedbackCommand {
	case class PublishFeedbackResults(
		notifications: Seq[Notification[AssignmentFeedback, Assignment]] = Nil,
		missingUsers: Seq[MissingUser] = Nil,
		badEmails: Seq[BadEmail] = Nil
	)
	type Command = Appliable[PublishFeedbackResults] with SelfValidating with GradeValidationResults

	case class MissingUser(universityId: String)
	case class BadEmail(user: User, exception: Exception = null)

	def apply(assignment: Assignment, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new PublishFeedbackCommandInternal(assignment, submitter, gradeGenerator)
			with ComposableCommand[PublishFeedbackResults]
			with AutowiringFeedbackForSitsServiceComponent
			with AutowiringUserLookupComponent
			with PublishFeedbackPermissions
			with PublishFeedbackValidation
			with PublishFeedbackDescription
			with PublishFeedbackNotification
			with PublishFeedbackNotificationCompletion
			with PublishFeedbackGradeValidationResults
			with QueuesFeedbackForSits
}

class PublishFeedbackCommandInternal(val assignment: Assignment, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[PublishFeedbackResults] with PublishFeedbackCommandRequest {
	self: QueuesFeedbackForSits with UserLookupComponent =>

	def applyInternal(): PublishFeedbackResults = {
		val allResults = feedbackToRelease.map { feedback =>
			feedback.released = true
			feedback.releasedDate = DateTime.now
			if (sendToSits) queueFeedback(feedback, submitter, gradeGenerator)
			generateNotification(userLookup.getUserByUserId(feedback.usercode), feedback)
		}

		allResults.foldLeft(PublishFeedbackResults()) { (acc, result) =>
			PublishFeedbackResults(
				notifications = acc.notifications ++ result.notifications,
				missingUsers = acc.missingUsers ++ result.missingUsers,
				badEmails = acc.badEmails ++ result.badEmails
			)
		}
	}

	private def generateNotification(user: User, feedback: Feedback) = {
		feedback match {
			case assignmentFeedback: AssignmentFeedback =>
				if (user.isFoundUser) {
					val email = user.getEmail
					if (email.hasText) {
						val n = Notification.init(new FeedbackPublishedNotification, submitter.apparentUser, Seq(assignmentFeedback), assignmentFeedback.assignment)
						n.recipientUniversityId = user.getUserId
						PublishFeedbackResults(
							notifications = Seq(n)
						)
					} else {
						PublishFeedbackResults(
							badEmails = Seq(PublishFeedbackCommand.BadEmail(user))
						)
					}
				} else {
					PublishFeedbackResults(
						missingUsers = Seq(PublishFeedbackCommand.MissingUser(user.getUserId))
					)
				}
			case _ => PublishFeedbackResults()
		}
	}
}

trait PublishFeedbackNotification extends Notifies[PublishFeedbackResults, Feedback] {
	override def emit(results: PublishFeedbackResults): Seq[Notification[AssignmentFeedback, Assignment]] = results.notifications
}

trait PublishFeedbackNotificationCompletion extends CompletesNotifications[PublishFeedbackResults] {
	self: PublishFeedbackCommandState with NotificationHandling =>

	def notificationsToComplete(commandResult: PublishFeedbackResults): CompletesNotificationsResult = {
		val feedbackNotifications = commandResult.notifications.flatMap(_.entities).flatMap(feedback =>
			notificationService.findActionRequiredNotificationsByEntityAndType[FinaliseFeedbackNotification](feedback)
		)
		if (!assignment.needsFeedbackPublishing) {
			CompletesNotificationsResult(
				feedbackNotifications ++
					notificationService.findActionRequiredNotificationsByEntityAndType[FeedbackDueGeneralNotification](assignment) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[FeedbackDueExtensionNotification](assignment),
				submitter.apparentUser
			)
		} else {
			CompletesNotificationsResult(feedbackNotifications, submitter.apparentUser)
		}
	}
}

trait PublishFeedbackCommandState extends SelectedStudentsState {
	def assignment: Assignment
	def submitter: CurrentUser
	def gradeGenerator: GeneratesGradesFromMarks
}

trait PublishFeedbackCommandRequest extends SelectedStudentsRequest with PublishFeedbackCommandState {
	var confirm: Boolean = false
	var sendToSits: Boolean = false

	def feedbackToRelease: Seq[Feedback] = feedbacks.filterNot { f => f.isPlaceholder || f.released }
}

trait GradeValidationResults {
	def validateGrades: ValidateAndPopulateFeedbackResult
}

trait PublishFeedbackGradeValidationResults extends GradeValidationResults {
	self: PublishFeedbackCommandRequest with FeedbackForSitsServiceComponent =>

	lazy val validateGrades: ValidateAndPopulateFeedbackResult =
		feedbackForSitsService.validateAndPopulateFeedback(feedbackToRelease, gradeGenerator)
}

trait QueuesFeedbackForSits {
	self: FeedbackForSitsServiceComponent =>

	def queueFeedback(feedback: Feedback, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks): Option[FeedbackForSits] =
		feedbackForSitsService.queueFeedback(feedback, submitter, gradeGenerator)
}

trait PublishFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: PublishFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.Publish, mandatory(assignment))
	}
}

trait PublishFeedbackValidation extends SelfValidating {
	self: PublishFeedbackCommandRequest =>

	override def validate(errors: Errors) {
		if (!assignment.openEnded && !assignment.isClosed) {
			errors.reject("feedback.publish.notclosed")
		} else if (feedbackToRelease.isEmpty) {
			errors.reject("feedback.publish.nofeedback")
		}

		if (!confirm) {
			errors.rejectValue("confirm", "feedback.publish.confirm")
		}
	}
}

trait PublishFeedbackDescription extends Describable[PublishFeedbackResults] {
	self: PublishFeedbackCommandRequest with UserLookupComponent =>

	override lazy val eventName: String = "PublishFeedback"

	override def describe(d: Description) {
		val students = userLookup.getUsersByUserIds(feedbackToRelease.map(_.usercode)).values.toSeq
		d.assignment(assignment)
			.studentIds(students.flatMap(m => Option(m.getWarwickId)))
			.studentUsercodes(students.map(_.getUserId))
	}
}