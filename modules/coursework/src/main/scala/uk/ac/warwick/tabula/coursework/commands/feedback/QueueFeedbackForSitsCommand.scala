package uk.ac.warwick.tabula.coursework.commands.feedback

import org.joda.time.DateTime
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
			lazy val users = getUsersWithFeedbackToPublish
			val allQueuedFeedbacks = for {
				(studentId, user) <- users
				feedback <- assignment.fullFeedback.find { _.universityId == studentId }
			} yield {
				val feedbackForSits = feedbackForSitsDao.getByFeedback(feedback) match {
					case Some(existingFeedbackForSits: FeedbackForSits) =>
						// this feedback been published before
						existingFeedbackForSits
					case None =>
						// create a new object for this feedback in the queue
						val newFeedbackForSits = new FeedbackForSits
						newFeedbackForSits.firstCreatedOn = DateTime.now
						newFeedbackForSits
				}
				feedbackForSits.init(feedback, submitter.realUser) // initialise or re-initialise
				feedbackForSitsDao.saveOrUpdate(feedbackForSits)
				feedbackForSits				
			}
		allQueuedFeedbacks
		}
	}

	def getUsersWithFeedbackToPublish = feedbackService.getUsersForFeedback(assignment)

	def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(getUsersWithFeedbackToPublish map { case(userId, user) => user.getWarwickId })

}
