package uk.ac.warwick.tabula.commands.cm2.feedback

import javax.mail.MessagingException
import javax.mail.internet.InternetAddress

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.{SelectedStudentsRequest, SelectedStudentsState}
import uk.ac.warwick.tabula.commands.cm2.feedback.PublishFeedbackCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.{FeedbackDueExtensionNotification, FeedbackDueGeneralNotification, FeedbackPublishedNotification, FinaliseFeedbackNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.{FoundUser, NoUser}
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
	type Command = Appliable[PublishFeedbackResults] with SelfValidating with GradeValidationResults with RecipientChecking with SubmissionsReportGenerator

	abstract class RecipientReportItem(val universityId: String, val user: User, val good: Boolean)
	case class MissingUser(id: String) extends RecipientReportItem(id, null, false)
	case class BadEmail(u: User, exception: Exception = null) extends RecipientReportItem(u.getWarwickId, u, false)
	case class GoodUser(u: User) extends RecipientReportItem(u.getWarwickId, u, true)

	case class RecipientCheckReport(users: Seq[RecipientReportItem]) {
		def hasProblems: Boolean = users.exists(!_.good)
		def problems: Seq[RecipientReportItem] = users.filterNot(_.good)
	}

	case class SubmissionsReport(
		alreadyPublished: Set[String],
		feedbackOnly: Set[String],
		submissionOnly: Set[String],
		withoutAttachments: Set[String],
		withoutMarks: Set[String],
		plagiarised: Set[String],
		hasProblems: Boolean
	)

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
			with PublishFeedbackRecipientChecking
			with PublishFeedbackSubmissionsReportGenerator
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

trait RecipientChecking {
	def recipientCheckReport: RecipientCheckReport
}

trait PublishFeedbackRecipientChecking extends RecipientChecking {
	self: PublishFeedbackCommandRequest with UserLookupComponent =>

	private def isGoodEmail(email: String): Boolean = {
		try {
			new InternetAddress(email).validate()
			true
		} catch {
			case e: MessagingException => false
		}
	}

	lazy val recipientCheckReport: RecipientCheckReport =
		RecipientCheckReport(feedbackToRelease.map { feedback =>
			userLookup.getUserByUserId(feedback.usercode) match {
				case FoundUser(u) =>
					if (u.getEmail.hasText && isGoodEmail(u.getEmail)) {
						GoodUser(u)
					} else {
						BadEmail(u)
					}
				case NoUser(_) => MissingUser(feedback.usercode)
			}
		})
}

trait SubmissionsReportGenerator {
	def submissionsReport: SubmissionsReport
}

trait PublishFeedbackSubmissionsReportGenerator extends SubmissionsReportGenerator {
	self: PublishFeedbackCommandRequest =>

	lazy val submissionsReport: SubmissionsReport = {
		val alreadyPublished = feedbacks.diff(feedbackToRelease).filterNot(_.isPlaceholder).map(_.usercode).toSet

		val feedbackUsercodes = feedbacks.filterNot(_.isPlaceholder).map(_.usercode).toSet
		val submissionUsercodes = submissions.map(_.usercode).toSet

		// Subtract the sets from each other to obtain discrepancies
		val feedbackOnly: Set[String] = feedbackUsercodes &~ submissionUsercodes
		val submissionOnly: Set[String] = submissionUsercodes &~ feedbackUsercodes

		/**
			* We want to show a warning if some feedback items are missing either marks or attachments
			* If however, all feedback items have only marks or attachments then we don't send a warning.
			*/
		val withoutAttachments: Set[String] = feedbacks
			.filter(f => !f.hasAttachments && !f.comments.exists(_.hasText))
			.map(_.usercode).toSet
		val withoutMarks: Set[String] = feedbacks.filter(!_.hasMarkOrGrade).map(_.usercode).toSet
		val plagiarised: Set[String] = submissions.filter(_.suspectPlagiarised).map(_.usercode).toSet

		val hasProblems: Boolean = {
			val shouldBeEmpty = Set(feedbackOnly, submissionOnly, plagiarised)
			val problems = assignment.collectSubmissions && shouldBeEmpty.exists { _.nonEmpty }

			if (assignment.collectMarks) {
				val shouldBeEmptyWhenCollectingMarks = Set(withoutAttachments, withoutMarks)
				problems || shouldBeEmptyWhenCollectingMarks.exists { _.nonEmpty }
			} else {
				problems
			}
		}

		SubmissionsReport(
			alreadyPublished,
			feedbackOnly,
			submissionOnly,
			withoutAttachments,
			withoutMarks,
			plagiarised,
			hasProblems
		)
	}
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