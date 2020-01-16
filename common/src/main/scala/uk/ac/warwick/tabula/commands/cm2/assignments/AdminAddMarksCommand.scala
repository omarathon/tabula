package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.{AddMarksCommandBindListener, AddMarksState}
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackChangeNotification
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, Notification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.docconversion.{AutowiringMarksExtractorComponent, MarkItem}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object AdminAddMarksCommand {
  def apply(assignment: Assignment, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
    new AdminAddMarksCommandInternal(assignment, submitter, gradeGenerator)
      with ComposableCommand[Seq[Feedback]]
      with AddMarksCommandBindListener
      with AdminAddMarksPermissions
      with AdminAddMarksDescription
      with AdminAddMarksNotifications
      with AutowiringMarksExtractorComponent
      with AutowiringFeedbackServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
}

abstract class AdminAddMarksCommandInternal(val assignment: Assignment, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
  extends CommandInternal[Seq[Feedback]] with AdminAddMarksState {

  self: FeedbackServiceComponent with AssessmentMembershipServiceComponent with FeedbackServiceComponent =>

  def isModified(markItem: MarkItem): Boolean = {
    markItem.currentFeedback(assignment).exists(_.hasContent)
  }

  def canMark(markItem: MarkItem): Boolean = true

  def applyInternal(): Seq[Feedback] = {
    def saveFeedback(markItem: MarkItem) = {
      markItem.user(assignment).map(u => {
        val feedback = markItem.currentFeedback(assignment).getOrElse {
          val newFeedback = new Feedback
          newFeedback.assignment = assignment
          newFeedback.uploaderId = submitter.apparentId
          newFeedback.usercode = u.getUserId
          newFeedback._universityId = u.getWarwickId
          newFeedback.released = false
          newFeedback.createdDate = DateTime.now
          newFeedback
        }
        feedback.actualMark = markItem.actualMark.maybeText.map(_.toInt)
        feedback.actualGrade = markItem.actualGrade.maybeText
        markItem.fieldValues.asScala.foreach { case (fieldName, value) =>
          feedback.setFieldValue(fieldName, value)
        }
        feedback.updatedDate = DateTime.now
        feedbackService.saveOrUpdate(feedback)
        feedback
      })
    }

    // persist valid marks
    marks.asScala.toSeq.filter(_.isValid).flatMap(saveFeedback)
  }
}

trait AdminAddMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AdminAddMarksState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Feedback.Manage, assignment)
  }
}

trait AdminAddMarksDescription extends Describable[Seq[Feedback]] {
  self: AdminAddMarksState =>

  override lazy val eventName = "AdminAddMarks"

  override def describe(d: Description): Unit = {
    d.assignment(assignment)
  }

  override def describeResult(d: Description, result: Seq[Feedback]): Unit = {
    d.assignment(assignment)
    d.studentIds(result.map(_.studentIdentifier))
  }
}

trait AdminAddMarksNotifications extends Notifies[Seq[Feedback], Feedback] {

  self: AdminAddMarksState =>

  def emit(updatedFeedback: Seq[Feedback]): Seq[FeedbackChangeNotification] = updatedReleasedFeedback.flatMap { feedback =>
    Option(Notification.init(new FeedbackChangeNotification, submitter.apparentUser, feedback, feedback.assignment))
  }
}

trait AdminAddMarksState extends AddMarksState with FeedbackServiceComponent {
  self: AssessmentMembershipServiceComponent =>
  def updatedReleasedFeedback: Seq[Feedback] = marks.asScala.toSeq.filter(_.isModified).flatMap(_.currentFeedback(assignment)).filter(_.released)
}
