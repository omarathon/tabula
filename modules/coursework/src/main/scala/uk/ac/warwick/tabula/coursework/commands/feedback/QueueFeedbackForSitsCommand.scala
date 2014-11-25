package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.data.FeedbackForSitsDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.{Command, Description}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import language.implicitConversions
import uk.ac.warwick.tabula.CurrentUser


class QueueFeedbackForSitsCommand(val assignment: Assignment, val submitter: CurrentUser)
	extends Command[Seq[FeedbackForSits]] {

	PermissionCheck(Permissions.Feedback.Publish, assignment)

	var feedbackService = Wire[FeedbackService]
	var feedbackForSitsDao = Wire[FeedbackForSitsDao]

	var confirm: Boolean = false

	def applyInternal() = {
		transactional() {
			val users = getUsersForFeedback
			val allResults = for {
				(studentId, user) <- users
				feedback <- assignment.fullFeedback.find { _.universityId == studentId }
			} yield {
				val feedbackForSits = new FeedbackForSits
				feedbackForSits.init(feedback, submitter)
				feedbackForSitsDao.saveOrUpdate(feedbackForSits)
				feedbackForSits
			}
		allResults
		}
	}

	def getUsersForFeedback = feedbackService.getUsersForFeedback(assignment)

	def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(getUsersForFeedback map { case(userId, user) => user.getWarwickId })

}
