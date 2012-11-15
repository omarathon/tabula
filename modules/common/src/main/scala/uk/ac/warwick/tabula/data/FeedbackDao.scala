package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.Feedback
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.Assignment

trait FeedbackDao {
	def getFeedback(id: String): Option[Feedback]
	def getFeedbackByUniId(assignment: Assignment, uniId: String): Option[Feedback]
	def delete(feedback: Feedback)
}

@Repository
class FeedbackDaoImpl extends FeedbackDao with Daoisms {

	private val clazz = classOf[Feedback].getName

	override def getFeedback(id: String) = getById[Feedback](id)

	override def getFeedbackByUniId(assignment: Assignment, uniId: String): Option[Feedback] =
		session.newCriteria[Feedback]
			.add(is("universityId", uniId))
			.add(is("assignment", assignment))
			.uniqueResult

	override def delete(feedback: Feedback) = session.delete(feedback)

}