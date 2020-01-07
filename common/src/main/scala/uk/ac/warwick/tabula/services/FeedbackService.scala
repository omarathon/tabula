package uk.ac.warwick.tabula.services

import org.hibernate.FetchMode

import scala.jdk.CollectionConverters._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire

trait FeedbackService {
  def getStudentFeedback(assessment: Assessment, usercode: String): Option[Feedback]

  def loadFeedbackForAssignment(assignment: Assignment): Seq[Feedback]

  def countPublishedFeedback(assignment: Assignment): Int

  def getUsersForFeedback(assignment: Assignment): Seq[(String, User)]

  def getFeedbackByUsercode(assignment: Assignment, usercode: String): Option[Feedback]

  def getFeedbackById(feedbackId: String): Option[Feedback]

  def getMarkerFeedbackById(markerFeedbackId: String): Option[MarkerFeedback]

  def getRejectedMarkerFeedbackByFeedback(feedback: Feedback): Seq[MarkerFeedback]

  def saveOrUpdate(feedback: Feedback): Unit

  def saveOrUpdate(mark: Mark): Unit

  def delete(feedback: Feedback): Unit

  def save(feedback: MarkerFeedback): Unit

  def delete(feedback: MarkerFeedback): Unit

  def addAnonymousIds(feedbacks: Seq[Feedback]): Seq[Feedback]
}

@Service(value = "feedbackService")
class FeedbackServiceImpl extends FeedbackService with Daoisms with Logging {

  @Autowired var userLookup: UserLookupService = _
  @Autowired var dao: FeedbackDao = _

  /* get users whose feedback is not published and who have not submitted work suspected
   * of being plagiarised */
  def getUsersForFeedback(assignment: Assignment): Seq[(String, User)] = {
    val plagiarisedSubmissions = assignment.submissions.asScala.filter { submission => submission.suspectPlagiarised }
    val plagiarisedIds = plagiarisedSubmissions.map(_.usercode)
    val unreleasedIds = assignment.unreleasedFeedback.map(_.usercode)
    val unplagiarisedUnreleasedIds = unreleasedIds.filter { usercode => !plagiarisedIds.contains(usercode) }
    userLookup.usersByUserIds(unplagiarisedUnreleasedIds).toSeq
  }

  def getStudentFeedback(assessment: Assessment, usercode: String): Option[Feedback] = {
    assessment.findFullFeedback(usercode)
  }

  def loadFeedbackForAssignment(assignment: Assignment): Seq[Feedback] =
    session.newCriteria[Feedback]
      .add(is("assignment", assignment))
      .setFetchMode("_marks", FetchMode.JOIN)
      .setFetchMode("markerFeedback", FetchMode.JOIN)
      .setFetchMode("markerFeedback.attachments", FetchMode.JOIN)
      .setFetchMode("markerFeedback.customFormValues", FetchMode.JOIN)
      .setFetchMode("outstandingStages", FetchMode.JOIN)
      .setFetchMode("customFormValues", FetchMode.JOIN)
      .setFetchMode("attachments", FetchMode.JOIN)
      .distinct
      .seq

  def countPublishedFeedback(assignment: Assignment): Int = {
    session.createSQLQuery("""select count(*) from feedback where assignment_id = :assignmentId and released = true""")
      .setString("assignmentId", assignment.id)
      .uniqueResult
      .asInstanceOf[Number].intValue
  }

  def getFeedbackByUsercode(assignment: Assignment, usercode: String): Option[Feedback] = transactional(readOnly = true) {
    dao.getFeedbackByUsercode(assignment, usercode)
  }

  def getFeedbackById(feedbackId: String): Option[Feedback] = {
    dao.getFeedback(feedbackId)
  }

  def getMarkerFeedbackById(markerFeedbackId: String): Option[MarkerFeedback] = {
    dao.getMarkerFeedback(markerFeedbackId)
  }

  def getRejectedMarkerFeedbackByFeedback(feedback: Feedback): Seq[MarkerFeedback] = {
    dao.getRejectedMarkerFeedbackByFeedback(feedback)
  }

  def delete(feedback: Feedback): Unit = transactional() {
    dao.delete(feedback)
  }

  def saveOrUpdate(feedback: Feedback): Unit = {
    session.saveOrUpdate(feedback)
  }

  def saveOrUpdate(mark: Mark): Unit = {
    session.saveOrUpdate(mark)
  }

  def save(feedback: MarkerFeedback): Unit = transactional() {
    dao.save(feedback)
  }

  def delete(markerFeedback: MarkerFeedback): Unit = transactional() {
    // remove link to parent
    val parentFeedback = markerFeedback.feedback
    parentFeedback.markerFeedback.remove(markerFeedback)
    saveOrUpdate(parentFeedback)
    dao.delete(markerFeedback)
  }

  def addAnonymousIds(feedbacks: Seq[Feedback]): Seq[Feedback] = transactional() {
    val assignments = feedbacks.map(_.assignment).distinct
    if (assignments.length > 1) throw new IllegalArgumentException("Can only generate IDs for feedback from the same assignment")
    assignments.headOption.foreach(assignment => {
      val nextIndex = dao.getLastAnonIndex(assignment) + 1

      // add IDs to any feedback that doesn't already have one
      for ((feedback, i) <- feedbacks.filter(_.anonymousId.isEmpty).zipWithIndex) {
        feedback.anonymousId = Some(nextIndex + i)
        dao.save(feedback)
      }
    })
    feedbacks
  }

}

trait FeedbackServiceComponent {
  def feedbackService: FeedbackService
}

trait AutowiringFeedbackServiceComponent extends FeedbackServiceComponent {
  var feedbackService: FeedbackService = Wire[FeedbackService]
}
