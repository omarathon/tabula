package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.userlookup.User

@Entity
@Proxy
@DiscriminatorValue(value = "Cm2MarkedPlagiarised")
class Cm2MarkedPlagiarisedNotification extends NotificationWithTarget[Submission, Assignment]
  with SingleItemNotification[Submission] with AutowiringUserLookupComponent
  with MyWarwickActivity with BatchedNotification[NotificationWithTarget[Submission, Assignment] with Cm2MarkedPlagiarisedNotification] {

  def submission: Submission = item.entity

  def assignment: Assignment = target.entity

  def module: Module = assignment.module

  def moduleCode: String = module.code.toUpperCase

  def student: User = userLookup.getUserByUserId(submission.usercode)

  priority = Warning

  def recipients: Seq[User] = {
    val moduleManagers = module.managers
    val departmentAdmins = module.adminDepartment.owners

    moduleManagers.users.toSeq ++ departmentAdmins.users.toSeq
  }

  def url: String = Routes.cm2.admin.assignment.submissionsandfeedback(assignment)

  def urlTitle = "view the submissions for this assignment"

  def title: String = "%s: A submission by %s for \"%s\" is suspected of plagiarism".format(moduleCode, submission.studentIdentifier, assignment.name)

  def verb = "Mark plagiarised"

  def content = FreemarkerModel("/WEB-INF/freemarker/emails/suspectPlagiarism.ftl", Map(
    "submission" -> submission,
    "assignment" -> assignment,
    "module" -> module,
    "student" -> student
  ))

  override def titleForBatchInternal(notifications: Seq[NotificationWithTarget[Submission, Assignment] with Cm2MarkedPlagiarisedNotification], user: User): String = {
    if (notifications.length > 1) s"${notifications.length} assignment submissions are suspected of plagiarism" else "1 assignment submission is suspected of plagiarism"
  }

  override def contentForBatchInternal(notifications: Seq[NotificationWithTarget[Submission, Assignment] with Cm2MarkedPlagiarisedNotification]): FreemarkerModel = {
    val byAssignment = groupedByAssignment(notifications)
    FreemarkerModel("/WEB-INF/freemarker/emails/marked_plagiarised_batch.ftl", Map(
      "batches" -> byAssignment.map { case (assignmentName, batch) =>
        Map(
          "assignmentName" -> assignmentName,
          "submissions" -> batch.map(_.content.model.get("submission"))
        )
      }
    ))
  }

  override def urlForBatchInternal(notifications: Seq[NotificationWithTarget[Submission, Assignment] with Cm2MarkedPlagiarisedNotification], user: User): String = {
    Routes.cm2.home
  }

  override def urlTitleForBatchInternal(notifications: Seq[NotificationWithTarget[Submission, Assignment] with Cm2MarkedPlagiarisedNotification]): String = {
    "open Coursework Management"
  }

  private def groupedByAssignment(batch: Seq[NotificationWithTarget[Submission, Assignment]]): Seq[(String, Seq[NotificationWithTarget[Submission, Assignment]])] =
    batch.groupBy(_.target.entity.name)
      .view
      .toSeq
}
