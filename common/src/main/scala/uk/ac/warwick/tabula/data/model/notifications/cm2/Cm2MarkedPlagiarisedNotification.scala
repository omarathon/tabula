package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

@Entity
@Proxy
@DiscriminatorValue(value = "Cm2MarkedPlagiarised")
class Cm2MarkedPlagiarisedNotification extends NotificationWithTarget[Submission, Assignment]
  with SingleItemNotification[Submission] with AutowiringUserLookupComponent
  with MyWarwickNotification {

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

  def url: String = Routes.admin.assignment.submissionsandfeedback(assignment)

  def urlTitle = "view the submissions for this assignment"

  def title: String = "%s: A submission by %s for \"%s\" is suspected of plagiarism".format(moduleCode, submission.studentIdentifier, assignment.name)

  def verb = "Mark plagiarised"

  def content = FreemarkerModel("/WEB-INF/freemarker/emails/suspectPlagiarism.ftl", Map(
    "submission" -> submission,
    "assignment" -> assignment,
    "module" -> module,
    "student" -> student
  ))
}
