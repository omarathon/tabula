package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{Assignment, BatchedNotificationHandler, BatchedNotificationWithTarget, FreemarkerModel, NotificationWithTarget, Submission, UserSettings}
import uk.ac.warwick.tabula.data.model.permissions.{GrantedPermission, RoleOverride}
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.services.{SecurityService, UserSettingsService}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.Seq
import scala.reflect.ClassTag

@Entity
@Proxy
@DiscriminatorValue("SubmissionReceived")
class SubmissionReceivedNotification
  extends BatchedNotificationWithTarget[Submission, Assignment, SubmissionReceivedNotification](SubmissionReceivedBatchedNotificationHandler)
    with SubmissionNotification {

  override def onPreSave(isNew: Boolean): Unit = {
    // if this submission was noteworthy then the priority is higher
    if (submission.isNoteworthy) {
      priority = Warning
    }
  }

  @transient
  var userSettings: UserSettingsService = Wire[UserSettingsService]

  @transient
  var permissionsService: PermissionsService = Wire[PermissionsService]

  @transient
  var securityService: SecurityService = Wire[SecurityService]

  def templateLocation = "/WEB-INF/freemarker/emails/submissionnotify.ftl"

  def submissionTitle: String =
    if (submission == null) "Submission"
    else if (submission.isAuthorisedLate) "Authorised late submission"
    else if (submission.isLate) "Late submission"
    else "Submission"

  def title: String = "%s: %s received for \"%s\"".format(moduleCode, submissionTitle, assignment.name)

  def canEmailUser(user: User): Boolean = {
    // Alert on noteworthy submissions by default
    val setting = userSettings.getByUserId(user.getUserId).map(_.alertsSubmission).getOrElse(UserSettings.AlertsNoteworthySubmissions)

    setting match {
      case UserSettings.AlertsAllSubmissions => true
      case UserSettings.AlertsNoteworthySubmissions => submission.isNoteworthy
      case _ => false
    }
  }

  def url: String = Routes.admin.assignment.submissionsandfeedback.list(assignment)

  override def urlFor(user: User): String = {
    val feedback = assignment.findFeedback(submission.usercode)

    if (assignment.hasWorkflow && feedback.toSeq.flatMap(_.allMarkerFeedback).map(_.marker).contains(user)) {
      Routes.admin.assignment.markerFeedback(assignment, user)
    } else {
      Routes.admin.assignment.submissionsandfeedback.list(assignment)
    }
  }

  def urlTitle = "view all submissions for this assignment"

  def recipients: Seq[User] = {
    // TAB-2333 Get any user that has Submission.Delete over the submission, or any of its permission parents
    // Look at Assignment, Module and Department (can't grant explicitly over one Submission atm)
    val requiredPermission = Permissions.Submission.Delete

    def usersWithPermission[A <: PermissionsTarget : ClassTag](scope: A) = {
      val roleGrantedUsers =
        permissionsService.getAllGrantedRolesFor[A](scope)
          .filter(_.mayGrant(requiredPermission))
          .flatMap(_.users.users)
          .toSet

      val explicitlyGrantedUsers =
        permissionsService.getGrantedPermission(scope, requiredPermission, RoleOverride.Allow)
          .toSet
          .flatMap { permission: GrantedPermission[A] => permission.users.users }

      roleGrantedUsers ++ explicitlyGrantedUsers
    }

    def withParents(target: PermissionsTarget): LazyList[PermissionsTarget] = target #:: target.permissionsParents.flatMap(withParents)

    val adminsWithPermission = withParents(assignment).flatMap(usersWithPermission)
      .filter { user => securityService.can(new CurrentUser(user, user), requiredPermission, submission) }

    // Contact the current marker, if there is one, and the submission has already been released
    val feedback = assignment.findFeedback(submission.usercode)

    val currentMarker = if (assignment.hasWorkflow) {
      feedback.toSeq.flatMap(_.markingInProgress).map(_.marker)
    } else {
      Seq()
    }

    (adminsWithPermission ++ currentMarker).distinct.filter(canEmailUser)
  }

}

object SubmissionReceivedBatchedNotificationHandler extends BatchedNotificationHandler[SubmissionReceivedNotification] {
  // Batch notifications for the same assignment
  override def groupBatchInternal(notifications: Seq[SubmissionReceivedNotification]): Seq[Seq[SubmissionReceivedNotification]] =
    notifications.groupBy(_.assignment).values.toSeq

  override def titleForBatchInternal(notifications: Seq[SubmissionReceivedNotification], user: User): String = {
    val submissions = notifications.map(_.submission)
    val authorisedLateSubmissions = submissions.count(s => s != null && s.isAuthorisedLate)
    val lateSubmissions = submissions.count(s => s != null && !s.isAuthorisedLate && s.isLate)
    val otherSubmissions = submissions.size - authorisedLateSubmissions - lateSubmissions

    val parts = Seq(
      lateSubmissions match {
        case 0 => None
        case 1 => Some("1 late submission")
        case n => Some(s"$n late submissions")
      },
      authorisedLateSubmissions match {
        case 0 => None
        case 1 => Some("1 authorised late submission")
        case n => Some(s"$n authorised late submissions")
      },
      otherSubmissions match {
        case 0 => None
        case 1 => Some("1 submission")
        case n => Some(s"$n submissions")
      }
    ).flatten.mkString(", ")

    "%s: %s received for \"%s\"".format(notifications.head.moduleCode, parts, notifications.head.assignment.name)
  }

  override def contentForBatchInternal(notifications: Seq[SubmissionReceivedNotification]): FreemarkerModel =
    FreemarkerModel("/WEB-INF/freemarker/emails/submissionnotify_batch.ftl", Map(
      "assignment" -> notifications.head.assignment,
      "module" -> notifications.head.module,
      "submissions" -> notifications.sortBy(_.submission.submittedDate).map { notification =>
        Map(
          "submission" -> notification.submission,
          "submissionDate" -> DateFormats.NotificationDateTime.print(notification.submission.submittedDate),
          "feedbackDeadlineDate" -> notification.submission.feedbackDeadline.map(DateFormats.NotificationDateOnly.print),
        )
      }
    ))

  override def urlForBatchInternal(notifications: Seq[SubmissionReceivedNotification], user: User): String =
    Routes.admin.assignment.submissionsandfeedback.list(notifications.head.assignment)

  override def urlTitleForBatchInternal(notifications: Seq[SubmissionReceivedNotification]): String =
    "view all submissions for this assignment"
}
