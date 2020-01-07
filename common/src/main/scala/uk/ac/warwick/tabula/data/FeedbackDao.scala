package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Projections
import uk.ac.warwick.tabula.data.model._
import org.springframework.stereotype.Repository

trait FeedbackDao {
  def getFeedback(id: String): Option[Feedback]

  def getFeedbackByUsercode(assignment: Assignment, usercode: String): Option[Feedback]

  def getMarkerFeedback(id: String): Option[MarkerFeedback]

  def getRejectedMarkerFeedbackByFeedback(feedback: Feedback): Seq[MarkerFeedback]

  def save(feedback: Feedback): Unit

  def delete(feedback: Feedback): Unit

  def save(feedback: MarkerFeedback): Unit

  def delete(feedback: MarkerFeedback): Unit

  def getLastAnonIndex(assignment: Assignment): Int
}

abstract class AbstractFeedbackDao extends FeedbackDao with Daoisms {
  self: ExtendedSessionComponent =>

  override def getFeedback(id: String): Option[Feedback] = getById[Feedback](id)

  override def getMarkerFeedback(id: String): Option[MarkerFeedback] = getById[MarkerFeedback](id)

  override def getRejectedMarkerFeedbackByFeedback(feedback: Feedback): Seq[MarkerFeedback] =
    session.newCriteria[MarkerFeedback]
      .add(is("state", MarkingState.Rejected))
      .add(is("feedback", feedback))
      .seq

  override def getFeedbackByUsercode(assignment: Assignment, usercode: String): Option[Feedback] =
    session.newCriteria[Feedback]
      .add(is("usercode", usercode))
      .add(is("assignment", assignment))
      .uniqueResult

  override def save(feedback: Feedback): Unit = {
    session.saveOrUpdate(feedback)
  }

  override def delete(feedback: Feedback): Unit = {
    feedback.clearAttachments()
    session.delete(feedback)
  }

  override def save(feedback: MarkerFeedback): Unit = {
    session.saveOrUpdate(feedback)
  }

  override def delete(feedback: MarkerFeedback): Unit = {
    session.delete(feedback)
  }

  override def getLastAnonIndex(assignment: Assignment): Int = {
    session.newCriteria[Feedback]
      .add(is("assignment", assignment))
      .project[Option[Int]](Projections.max("anonymousId"))
      .uniqueResult
      .flatten
      .getOrElse(0)
  }


}

@Repository
class FeedbackDaoImpl extends AbstractFeedbackDao with Daoisms