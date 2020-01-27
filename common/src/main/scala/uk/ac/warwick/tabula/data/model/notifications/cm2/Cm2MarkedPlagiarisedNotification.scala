package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.userlookup.User

object Cm2MarkedPlagiarisedNotification {
  val templateLocation = "/WEB-INF/freemarker/emails/suspectPlagiarism.ftl"
  val batchTemplateLocation = "/WEB-INF/freemarker/emails/marked_plagiarised_batch.ftl"
}

@Entity
@Proxy
@DiscriminatorValue(value = "Cm2MarkedPlagiarised")
class Cm2MarkedPlagiarisedNotification
  extends BatchedNotificationWithTarget[Submission, Assignment, Cm2MarkedPlagiarisedNotification](Cm2MarkedPlagiarisedBatchedNotificationHandler)
    with SingleItemNotification[Submission]
    with AutowiringUserLookupComponent
    with MyWarwickActivity {

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

  def content: FreemarkerModel = FreemarkerModel(Cm2MarkedPlagiarisedNotification.templateLocation, Map(
    "submission" -> submission,
    "assignment" -> assignment,
    "module" -> module,
    "student" -> student
  ))
}

/**
 * These are grouped by assignment so we know they're all for the same assignment and batching guarantees
 * this will only be called for notifications.size > 1
 */
object Cm2MarkedPlagiarisedBatchedNotificationHandler extends BatchedNotificationHandler[Cm2MarkedPlagiarisedNotification] {
  override def groupBatchInternal(notifications: Seq[Cm2MarkedPlagiarisedNotification]): Seq[Seq[Cm2MarkedPlagiarisedNotification]] =
    notifications.groupBy(_.assignment).values.toSeq

  override def titleForBatchInternal(notifications: Seq[Cm2MarkedPlagiarisedNotification], user: User): String =
    s"""${notifications.head.moduleCode}: ${notifications.size} submissions for "${notifications.head.assignment.name}" are suspected of plagiarism"""

  override def contentForBatchInternal(notifications: Seq[Cm2MarkedPlagiarisedNotification]): FreemarkerModel =
    FreemarkerModel(Cm2MarkedPlagiarisedNotification.batchTemplateLocation, Map(
      "assignment" -> notifications.head.assignment,
      "module" -> notifications.head.module,
      "submissions" -> notifications.map(_.submission)
    ))

  override def urlForBatchInternal(notifications: Seq[Cm2MarkedPlagiarisedNotification], user: User): String =
    Routes.cm2.admin.assignment.submissionsandfeedback(notifications.head.assignment)

  override def urlTitleForBatchInternal(notifications: Seq[Cm2MarkedPlagiarisedNotification]): String =
    "view the submissions for this assignment"
}
