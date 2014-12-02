package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.{Notifies, Command, Description, SelfValidating}
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackPublishedNotification
import uk.ac.warwick.tabula.data.model.{Notification, Feedback, Assignment, Module}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{UserLookupService, FeedbackService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import language.implicitConversions
import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser

object PublishFeedbackCommand {
	case class MissingUser(universityId: String)
	case class BadEmail(user: User, exception: Exception = null)
	
	case class PublishFeedbackResults(
		notifications: Seq[Notification[Feedback, Assignment]] = Nil,
		missingUsers: Seq[MissingUser] = Nil,
		badEmails: Seq[BadEmail] = Nil
	)
}

class PublishFeedbackCommand(val module: Module, val assignment: Assignment, val submitter: CurrentUser)
	extends Command[PublishFeedbackCommand.PublishFeedbackResults] with Notifies[PublishFeedbackCommand.PublishFeedbackResults, Feedback] with SelfValidating {
	import PublishFeedbackCommand._

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Feedback.Publish, assignment)

	var feedbackService = Wire.auto[FeedbackService]
	var userLookup = Wire.auto[UserLookupService]

	var confirm: Boolean = false

	// validation done even when showing initial form.
	def prevalidate(errors: Errors) {
		if (!assignment.openEnded && !assignment.isClosed) {
			errors.reject("feedback.publish.notclosed")
		} else if (assignment.fullFeedback.isEmpty) {
			errors.reject("feedback.publish.nofeedback")
		}
	}

	def validate(errors: Errors) {
		prevalidate(errors)
		if (!confirm) {
			errors.rejectValue("confirm", "feedback.publish.confirm")
		}
	}

	def applyInternal() = {
		transactional() {
			if (assignment.uploadMarksToSits) {
				val cmd = new QueueFeedbackForSitsCommand(assignment, submitter)
				cmd.apply()
			}

			val users = getUsersForFeedback
			val allResults = for {
				(studentId, user) <- users
				feedback <- assignment.fullFeedback.find { _.universityId == studentId }
			} yield {
				feedback.released = true
				feedback.releasedDate = new DateTime
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

	def getUsersForFeedback = feedbackService.getUsersForFeedback(assignment)

	def describe(d: Description) = d 
		.assignment(assignment)
		.studentIds(getUsersForFeedback map { case(userId, user) => user.getWarwickId })

	def emit(results: PublishFeedbackResults) = results.notifications

}