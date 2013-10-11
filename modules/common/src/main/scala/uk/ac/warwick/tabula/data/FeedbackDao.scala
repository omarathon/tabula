package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.Daoisms._
import org.hibernate.criterion.Restrictions.{eq => is}
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Feedback, Assignment}
import org.springframework.stereotype.Repository

trait FeedbackDao {
	def getFeedback(id: String): Option[Feedback]
	def getFeedbackByUniId(assignment: Assignment, uniId: String): Option[Feedback]
	def getMarkerFeedback(id: String): Option[MarkerFeedback]
	def save(feedback: Feedback)
	def delete(feedback: Feedback)
	def save(feedback: MarkerFeedback)
	def delete(feedback: MarkerFeedback)
}

abstract class AbstractFeedbackDao extends FeedbackDao {
	self: ExtendedSessionComponent =>

	override def getFeedback(id: String) = getById[Feedback](id)
	override def getMarkerFeedback(id: String) = getById[MarkerFeedback](id)

	override def getFeedbackByUniId(assignment: Assignment, uniId: String): Option[Feedback] =
		session.newCriteria[Feedback]
			.add(is("universityId", uniId))
			.add(is("assignment", assignment))
			.uniqueResult

	override def save(feedback: Feedback) = {
		session.saveOrUpdate(feedback)
	}

	override def delete(feedback: Feedback) = {
		// We need to delete any markerfeedback first
		Option(feedback.firstMarkerFeedback) foreach { session.delete(_) }
		Option(feedback.secondMarkerFeedback) foreach { session.delete(_) }
		
		session.delete(feedback)
	}

	override def save(feedback: MarkerFeedback) = {
		session.saveOrUpdate(feedback)
	}

	override def delete(feedback: MarkerFeedback) = {
		session.delete(feedback)
	}

}

@Repository
class FeedbackDaoImpl extends AbstractFeedbackDao with Daoisms