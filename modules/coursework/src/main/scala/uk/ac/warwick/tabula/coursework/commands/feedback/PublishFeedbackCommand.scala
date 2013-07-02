package uk.ac.warwick.tabula.coursework.commands.feedback

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.{Notifies, Command, Description, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Notification, Feedback, Assignment, Module}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.FeedbackService
import language.implicitConversions
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.commands.assignments.notifications.FeedbackPublishedNotification
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.tabula.CurrentUser

class PublishFeedbackCommand(val module: Module, val assignment: Assignment, val submitter: CurrentUser)
	extends Command[Unit] with Notifies[Feedback] with SelfValidating {

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Feedback.Publish, assignment)

	var feedbackService = Wire.auto[FeedbackService]
	var userLookup = Wire.auto[UserLookupService]

	var confirm: Boolean = false

	case class MissingUser(universityId: String)
	case class BadEmail(user: User, exception: Exception = null)

	var missingUsers: JList[MissingUser] = JArrayList()
	var badEmails: JList[BadEmail] = JArrayList()
	var notifications: JList[Notification[Feedback]] = JArrayList()

	// validation done even when showing initial form.
	def prevalidate(errors: Errors) {
		if (!assignment.openEnded && !assignment.isClosed()) {
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

	def applyInternal() {
		transactional() {
			val users = getUsersForFeedback
			for ((studentId, user) <- users) {
				val feedbacks = assignment.fullFeedback.find { _.universityId == studentId }
				for (feedback <- feedbacks) {
					feedback.released = true
					feedback.releasedDate = new DateTime
					generateNotification(studentId, user, feedback)
				}
			}
		}
	}

	private def generateNotification(id:String, user:User, feedback:Feedback) {
		if (user.isFoundUser) {
			val email = user.getEmail
			if (email.hasText) {
				notifications.add(new FeedbackPublishedNotification(feedback, submitter.apparentUser, user) with FreemarkerTextRenderer)
			} else {
				badEmails.add(BadEmail(user))
			}
		} else {
			missingUsers.add(MissingUser(id))
		}
	}

	def getUsersForFeedback = feedbackService.getUsersForFeedback(assignment)

	def describe(d: Description) = d 
		.assignment(assignment)
		.studentIds(getUsersForFeedback map { case(userId, user) => user.getWarwickId })

	def emit: Seq[Notification[Feedback]] = notifications.asScala

}