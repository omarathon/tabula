package uk.ac.warwick.tabula.api.commands.coursework

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.api.web.controllers.coursework.assignments.{ApiAddMarksState, ApiAddMarksValidator, FeedbackItem}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description, Notifies}
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackChangeNotification
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, Feedback, Notification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringFeedbackServiceComponent, FeedbackServiceComponent, GeneratesGradesFromMarks}
import uk.ac.warwick.tabula.services.cm2.docconversion.{AutowiringMarksExtractorComponent, MarkItem}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object ApiAddMarksCommand {
  def apply(assignment: Assignment, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
    new ApiAddMarksCommandInternal(assignment, submitter, gradeGenerator)
      with ComposableCommand[Seq[Feedback]]
      with ApiAddMarksValidator
      with ApiAddMarksPermissions
      with ApiAddMarksDescription
      with ApiAddMarksNotifications
      with AutowiringMarksExtractorComponent
      with AutowiringFeedbackServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
}

abstract class ApiAddMarksCommandInternal(val assignment: Assignment, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
  extends CommandInternal[Seq[Feedback]] with ApiAddMarksState {

  self: FeedbackServiceComponent with AssessmentMembershipServiceComponent with FeedbackServiceComponent =>

  def isModified(feedbackItem: FeedbackItem): Boolean = {
    feedbackItem.currentFeedback(assignment).exists(_.hasContent)
  }

  def canMark(feedbackItem: FeedbackItem): Boolean = true

  def applyInternal(): Seq[Feedback] = {
    def saveFeedback(feedbackItem: FeedbackItem) = {
      feedbackItem.user(assignment).map(u => {
        val feedback = feedbackItem.currentFeedback(assignment).getOrElse {
          val newFeedback = new AssignmentFeedback
          newFeedback.assignment = assignment
          newFeedback.uploaderId = submitter.apparentId
          newFeedback.usercode = u.getUserId
          newFeedback._universityId = u.getWarwickId
          newFeedback.released = false
          newFeedback.createdDate = DateTime.now
          newFeedback
        }
        feedback.actualMark = feedbackItem.mark.maybeText.map(_.toInt)
        feedback.actualGrade = feedbackItem.grade.maybeText
        feedback.setFieldValue("feedbackText", feedbackItem.feedback)
        feedback.updatedDate = DateTime.now
        feedbackService.saveOrUpdate(feedback)
        feedback
      })
    }

    // persist valid marks
    students.asScala.toSeq.filter(_.isValid).flatMap(saveFeedback)
  }
}


trait ApiAddMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ApiAddMarksState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
  }
}

trait ApiAddMarksDescription extends Describable[Seq[Feedback]] {
  self: ApiAddMarksState =>

  override lazy val eventName = "ApiAddMarks"

  override def describe(d: Description) {
    d.assignment(assignment)
  }

  override def describeResult(d: Description, result: Seq[Feedback]): Unit = {
    d.assignment(assignment)
    d.studentIds(result.map(_.studentIdentifier))
  }
}

trait ApiAddMarksNotifications extends Notifies[Seq[Feedback], Feedback] {
  self: ApiAddMarksState =>

  def emit(updatedFeedback: Seq[Feedback]): Seq[FeedbackChangeNotification] = updatedReleasedFeedback.flatMap { feedback =>
    HibernateHelpers.initialiseAndUnproxy(feedback) match {
      case assignmentFeedback: AssignmentFeedback =>
        Option(Notification.init(new FeedbackChangeNotification, submitter.apparentUser, assignmentFeedback, assignmentFeedback.assignment))
      case _ =>
        None
    }
  }
}
