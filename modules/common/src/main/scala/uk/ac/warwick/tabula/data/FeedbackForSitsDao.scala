package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.{Feedback, FeedbackForSitsStatus, FeedbackForSits}
import uk.ac.warwick.spring.Wire

trait FeedbackForSitsDaoComponent {
	def feedbackForSitsDao: FeedbackForSitsDao
}

trait AutowiringFeedbackForSitsDaoComponent extends FeedbackForSitsDaoComponent {
	var feedbackForSitsDao = Wire[FeedbackForSitsDao]
}

trait FeedbackForSitsDao {
	def saveOrUpdate(feedbackForSits: FeedbackForSits)
	def saveOrUpdate(feedback: Feedback)
	def feedbackToLoad: Seq[FeedbackForSits]
	def getByFeedback(feedback: Feedback): Option[FeedbackForSits]
}

@Repository
class FeedbackForSitsDaoImpl extends FeedbackForSitsDao with Daoisms {

	def saveOrUpdate(feedbackForSits: FeedbackForSits) = session.saveOrUpdate(feedbackForSits)
	def saveOrUpdate(feedback: Feedback) = session.saveOrUpdate(feedback)

	def feedbackToLoad =
		session.newCriteria[FeedbackForSits]
			.add(isNot("status", FeedbackForSitsStatus.Successful))
			.seq

	def getByFeedback(feedback: Feedback) = {
		session.newCriteria[FeedbackForSits]
			.add(is("feedback", feedback))
			.uniqueResult
	}
}
