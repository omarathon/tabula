package uk.ac.warwick.tabula.services
import scala.collection.JavaConversions._

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.criterion.{Projections, Restrictions, Order}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import javax.persistence.Entity
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire

trait FeedbackService {
	def getStudentFeedback(assignment: Assignment, warwickId: String): Option[Feedback]
	def countPublishedFeedback(assignment: Assignment): Int
	def countFullFeedback(assignment: Assignment): Int
	def getUsersForFeedback(assignment: Assignment): Seq[Pair[String, User]]
	def getFeedbackByUniId(assignment: Assignment, uniId: String): Option[Feedback]
	def save(feedback: Feedback)
	def delete(feedback: Feedback)
}

@Service(value = "feedbackService")
class FeedbackServiceImpl extends FeedbackService with Daoisms with Logging {
	import Restrictions._

	@Autowired var userLookup: UserLookupService = _
	@Autowired var dao: FeedbackDao = _

	/* get users whose feedback is not published and who have not submitted work suspected
	 * of being plagiarised */
	def getUsersForFeedback(assignment: Assignment): Seq[Pair[String, User]] = {
		val plagiarisedSubmissions = assignment.submissions.filter { submission => submission.suspectPlagiarised }
		val plagiarisedIds = plagiarisedSubmissions.map { _.universityId }
		val unreleasedIds = assignment.unreleasedFeedback.map { _.universityId }
		val unplagiarisedUnreleasedIds = unreleasedIds.filter { uniId => !plagiarisedIds.contains(uniId) }
		unplagiarisedUnreleasedIds.par.map { (id) => (id, userLookup.getUserByWarwickUniId(id, false)) }.seq
	}

	def getStudentFeedback(assignment: Assignment, uniId: String) = {
		assignment.findFullFeedback(uniId)
	}

	def countPublishedFeedback(assignment: Assignment): Int = {
		session.createSQLQuery("""select count(*) from feedback where assignment_id = :assignmentId and released = 1""")
			.setString("assignmentId", assignment.id)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	def countFullFeedback(assignment: Assignment): Int = {
		session.createQuery("""select count(*) from Feedback f join f.attachments a
			where f.assignment = :assignment
			and not (actualMark is null and actualGrade is null and f.attachments is empty)""")
			.setEntity("assignment", assignment)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	def getFeedbackByUniId(assignment: Assignment, uniId: String) = transactional(readOnly = true) {
		dao.getFeedbackByUniId(assignment, uniId)
	}

	def save(feedback: Feedback) = transactional() {
		dao.save(feedback)
	}

	def delete(feedback: Feedback) = transactional() {
		dao.delete(feedback)
	}
}

trait FeedbackServiceComponent {
	def feedbackService: FeedbackService
}

trait AutowiringFeedbackServiceComponent extends FeedbackServiceComponent {
	var feedbackService = Wire[FeedbackService]
}
