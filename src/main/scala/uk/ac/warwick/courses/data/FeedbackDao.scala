package uk.ac.warwick.courses.data
import uk.ac.warwick.courses.data.model.Feedback
import org.springframework.stereotype.Repository
import uk.ac.warwick.courses.data.model.Assignment
import org.hibernate.criterion.{ Restrictions => is }

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
			.add(is eq ("universityId", uniId))
			.add(is eq ("assignment", assignment))
			.uniqueResult

	override def delete(feedback: Feedback) = session.delete(feedback)

}