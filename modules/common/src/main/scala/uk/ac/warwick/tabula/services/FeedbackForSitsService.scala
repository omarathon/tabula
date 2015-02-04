package uk.ac.warwick.tabula.services


import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.{FeedbackForSitsDaoComponent, AutowiringFeedbackForSitsDaoComponent}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.spring.Wire
import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser

trait FeedbackForSitsService {
	def saveOrUpdate(feedbackForSits: FeedbackForSits)
	def feedbackToLoad: Seq[FeedbackForSits]
	def getByFeedback(feedback: Feedback): Option[FeedbackForSits]
	def queueFeedback(feedback: Feedback, submitter: CurrentUser): FeedbackForSits
}

trait FeedbackForSitsServiceComponent {
	def feedbackForSitsService: FeedbackForSitsService
}

trait AutowiringFeedbackForSitsServiceComponent extends FeedbackForSitsServiceComponent {
	var feedbackForSitsService = Wire[FeedbackForSitsService]
}

abstract class AbstractFeedbackForSitsService extends FeedbackForSitsService {

	self: FeedbackForSitsDaoComponent =>

	def saveOrUpdate(feedbackForSits: FeedbackForSits) = feedbackForSitsDao.saveOrUpdate(feedbackForSits)
	def feedbackToLoad: Seq[FeedbackForSits] = feedbackForSitsDao.feedbackToLoad
	def getByFeedback(feedback: Feedback): Option[FeedbackForSits] = feedbackForSitsDao.getByFeedback(feedback)
	def queueFeedback(feedback: Feedback, submitter: CurrentUser):FeedbackForSits = {
		val feedbackForSits = getByFeedback(feedback) match {
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
		saveOrUpdate(feedbackForSits)
		feedbackForSits
	}

}

@Service("feedbackForSitsService")
class FeedbackForSitsServiceImpl
	extends AbstractFeedbackForSitsService
	with AutowiringFeedbackForSitsDaoComponent