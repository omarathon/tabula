package uk.ac.warwick.tabula.coursework.commands.feedback

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.coursework.{FeedbackDueGeneralNotification, FeedbackDueExtensionNotification, FeedbackPublishedNotification}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.permissions._
import language.implicitConversions
import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking, PermissionsCheckingMethods}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.coursework.commands.feedback.PublishFeedbackCommand.PublishFeedbackResults

object PublishFeedbackCommand {
	case class MissingUser(universityId: String)
	case class BadEmail(user: User, exception: Exception = null)
	
	case class PublishFeedbackResults(
		notifications: Seq[Notification[Feedback, Assignment]] = Nil,
		missingUsers: Seq[MissingUser] = Nil,
		badEmails: Seq[BadEmail] = Nil
	)

def apply(module: Module, assignment: Assignment, submitter: CurrentUser) =
	new PublishFeedbackCommandInternal(module, assignment, submitter)
		with ComposableCommand[PublishFeedbackCommand.PublishFeedbackResults]
		with AutowiringUserLookupComponent
		with AutowiringFeedbackServiceComponent
		with PublishFeedbackCommandState
		with PublishFeedbackPermissions
		with PublishFeedbackValidation
		with PublishFeedbackDescription
		with PublishFeedbackNotification
		with PublishFeedbackNotificationCompletion
		with QueuesFeedbackForSits
}

class PublishFeedbackCommandInternal(val module: Module, val assignment: Assignment, val submitter: CurrentUser)
	extends CommandInternal[PublishFeedbackCommand.PublishFeedbackResults]{

	self: PublishFeedbackCommandState with QueuesFeedbackForSits =>

	import PublishFeedbackCommand._

	def applyInternal() = {

			val allResults = for {
				(studentId, user) <- getUsersForFeedback
				feedback <- assignment.fullFeedback.find { _.universityId == studentId }
			} yield {
				feedback.released = true
				feedback.releasedDate = new DateTime
				if (sendToSits)	queueFeedback(feedback, submitter)
				generateNotification(studentId, user, feedback)
			}

		allResults.foldLeft(PublishFeedbackResults()) { (acc, result) =>
				PublishFeedbackResults(
					notifications = acc.notifications ++ result.notifications,
					missingUsers = acc.missingUsers ++ result.missingUsers,
					badEmails = acc.badEmails ++ result.badEmails
				)
			}
	}

	private def generateNotification(id:String, user:User, feedback:Feedback) = {
		if (user.isFoundUser) {
			val email = user.getEmail
			if (email.hasText) {
				val n = Notification.init(new FeedbackPublishedNotification, submitter.apparentUser, Seq(feedback), feedback.assignment)
				n.recipientUniversityId = user.getWarwickId
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
				missingUsers = Seq(PublishFeedbackCommand.MissingUser(id))
			)
		}
	}

}

trait PublishFeedbackNotification extends Notifies[PublishFeedbackCommand.PublishFeedbackResults, Feedback] {
	override def emit(results: PublishFeedbackResults) = results.notifications
}

trait PublishFeedbackNotificationCompletion extends CompletesNotifications[PublishFeedbackCommand.PublishFeedbackResults] {
	self: PublishFeedbackCommandState with NotificationHandling =>

	def notificationsToComplete(commandResult: PublishFeedbackCommand.PublishFeedbackResults): CompletesNotificationsResult = {
		if (!assignment.needsFeedbackPublishing) {
			CompletesNotificationsResult(
				notificationService.findActionRequiredNotificationsByEntityAndType[FeedbackDueGeneralNotification](assignment) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[FeedbackDueExtensionNotification](assignment),
				submitter.apparentUser
			)
		} else {
			EmptyCompletesNotificationsResult
		}
	}
}

trait PublishFeedbackCommandState {

	self: FeedbackServiceComponent =>

	val module: Module
	val assignment: Assignment
	val submitter: CurrentUser

	var confirm: Boolean = false
	val sendToSits: Boolean = false

	def getUsersForFeedback = feedbackService.getUsersForFeedback(assignment)

	// validation done even when showing initial form.
	def prevalidate(errors: Errors) {
		if (!assignment.openEnded && !assignment.isClosed) {
			errors.reject("feedback.publish.notclosed")
		} else if (assignment.fullFeedback.isEmpty) {
			errors.reject("feedback.publish.nofeedback")
		}
	}
}

trait QueuesFeedbackForSits extends AutowiringFeedbackForSitsServiceComponent {

	def queueFeedback(feedback: Feedback, submitter: CurrentUser) =
		feedbackForSitsService.queueFeedback(feedback, submitter)
}

trait PublishFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: PublishFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.Feedback.Publish, assignment)
	}
}

trait PublishFeedbackValidation extends SelfValidating {

	self: PublishFeedbackCommandState =>

	override def validate(errors: Errors) {
		prevalidate(errors)
		if (!confirm) {
			errors.rejectValue("confirm", "feedback.publish.confirm")
		}
	}
}

trait PublishFeedbackDescription extends Describable[PublishFeedbackCommand.PublishFeedbackResults] {

	self: PublishFeedbackCommandState =>

	override def describe(d: Description) {
		d.assignment(assignment)
		.studentIds(getUsersForFeedback map { case(userId, user) => user.getWarwickId })
	}
}