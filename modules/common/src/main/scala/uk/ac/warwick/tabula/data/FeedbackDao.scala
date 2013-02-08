package uk.ac.warwick.tabula.data

import model.{MarkerFeedback, Feedback, Assignment}
import org.springframework.stereotype.Repository

trait FeedbackDao {
	def getFeedback(id: String): Option[Feedback]
	def getFeedbackByUniId(assignment: Assignment, uniId: String): Option[Feedback]
	def getMarkerFeedback(id: String): Option[MarkerFeedback]
	def delete(feedback: Feedback)
}

@Repository
class FeedbackDaoImpl extends FeedbackDao with Daoisms {

	private val clazz = classOf[Feedback].getName

	override def getFeedback(id: String) = getById[Feedback](id)
	override def getMarkerFeedback(id: String) = getById[MarkerFeedback](id)

	override def getFeedbackByUniId(assignment: Assignment, uniId: String): Option[Feedback] =
		session.newCriteria[Feedback]
			.add(is("universityId", uniId))
			.add(is("assignment", assignment))
			.uniqueResult

	override def delete(feedback: Feedback) = session.delete(feedback)

}