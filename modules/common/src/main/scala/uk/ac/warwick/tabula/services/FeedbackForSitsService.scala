package uk.ac.warwick.tabula.services


import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.{FeedbackForSitsDaoComponent, AutowiringFeedbackForSitsDaoComponent}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.spring.Wire

trait FeedbackForSitsService {
	def saveOrUpdate(feedbackForSits: FeedbackForSits)
	def feedbackToLoad: Seq[FeedbackForSits]
	def getByFeedback(feedback: Feedback): Option[FeedbackForSits]
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

}

@Service("feedbackForSitsService")
class FeedbackForSitsServiceImpl
	extends AbstractFeedbackForSitsService
	with AutowiringFeedbackForSitsDaoComponent