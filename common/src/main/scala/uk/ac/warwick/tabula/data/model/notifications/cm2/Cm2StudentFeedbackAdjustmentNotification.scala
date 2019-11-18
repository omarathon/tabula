package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Info
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

object Cm2StudentFeedbackAdjustmentNotification {
  val templateLocation = "/WEB-INF/freemarker/emails/student_feedback_adjustment_notification.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("Cm2StudentFeedbackAdjustment")
class Cm2StudentFeedbackAdjustmentNotification
  extends NotificationWithTarget[AssignmentFeedback, Assignment]
    with SingleItemNotification[AssignmentFeedback]
    with SingleRecipientNotification
    with AutowiringUserLookupComponent
    with MyWarwickNotification {

  def verb = "adjusted"

  def assignment: Assignment = target.entity

  def feedback: Feedback = item.entity

  def recipient: User = userLookup.getUserByUserId(feedback.usercode)

  def whatAdjusted: String = {
    val mark = feedback.latestMark.map(m => "mark")
    val grade = feedback.latestGrade.map(g => "grade")
    (mark ++ grade).mkString(" and ")
  }

  def title = s"""${assignment.module.code.toUpperCase}: Adjustments have been made to your $whatAdjusted for "${assignment.name}""""

  def content = FreemarkerModel(Cm2StudentFeedbackAdjustmentNotification.templateLocation,
    Map(
      "assignment" -> assignment,
      "feedback" -> feedback,
      "whatAdjusted" -> whatAdjusted
    ))

  def url: String = Routes.assignment(assignment)

  def urlTitle = "view your feedback"

  priority = Info

}
