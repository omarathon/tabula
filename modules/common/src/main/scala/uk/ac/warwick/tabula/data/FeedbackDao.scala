package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.Daoisms._
import org.hibernate.criterion.Restrictions.{eq => is}
import uk.ac.warwick.tabula.data.model._
import org.springframework.stereotype.Repository
import uk.ac.warwick.userlookup.User

trait FeedbackDao {
	def getAssignmentFeedback(id: String): Option[AssignmentFeedback]
	def getAssignmentFeedbackByUniId(assignment: Assignment, uniId: String): Option[AssignmentFeedback]
	def getMarkerFeedback(id: String): Option[MarkerFeedback]
	def save(feedback: Feedback)
	def delete(feedback: Feedback)
	def save(feedback: MarkerFeedback)
	def delete(feedback: MarkerFeedback)
	def getExamFeedbackMap(exam: Exam, users: Seq[User]): Map[User, ExamFeedback]
}

abstract class AbstractFeedbackDao extends FeedbackDao {
	self: ExtendedSessionComponent =>

	override def getAssignmentFeedback(id: String) = getById[AssignmentFeedback](id)
	override def getMarkerFeedback(id: String) = getById[MarkerFeedback](id)

	override def getAssignmentFeedbackByUniId(assignment: Assignment, uniId: String): Option[AssignmentFeedback] =
		session.newCriteria[AssignmentFeedback]
			.add(is("universityId", uniId))
			.add(is("assignment", assignment))
			.uniqueResult

	override def save(feedback: Feedback) = {
		session.saveOrUpdate(feedback)
	}

	override def delete(feedback: Feedback) = {
		// We need to delete any markerfeedback first
		Option(feedback.firstMarkerFeedback) foreach { _.markDeleted() }
		Option(feedback.secondMarkerFeedback) foreach { _.markDeleted() }
		Option(feedback.thirdMarkerFeedback) foreach { _.markDeleted() }

		feedback.clearAttachments()
		
		session.delete(feedback)
	}

	override def save(feedback: MarkerFeedback) = {
		session.saveOrUpdate(feedback)
	}

	override def delete(feedback: MarkerFeedback) = {
		session.delete(feedback)
	}

	override def getExamFeedbackMap(exam: Exam, users: Seq[User]): Map[User, ExamFeedback] = {
		session.newCriteria[ExamFeedback]
			.add(is("exam", exam))
			.add(safeIn("universityId", users.map(_.getWarwickId)))
			.seq
			.groupBy(_.universityId)
			.map{case(universityId, feedbacks) =>
				users.find(_.getWarwickId == universityId).get -> feedbacks.head
			}

	}


}

@Repository
class FeedbackDaoImpl extends AbstractFeedbackDao with Daoisms