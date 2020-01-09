package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Info
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object FeedbackAdjustmentNotification {
  val templateLocation = "/WEB-INF/freemarker/emails/feedback_adjustment_notification.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("FeedbackAdjustment")
class FeedbackAdjustmentNotification
  extends NotificationWithTarget[Feedback, Assignment]
    with SingleItemNotification[Feedback]
    with AutowiringUserLookupComponent
    with MyWarwickActivity {

  def verb = "adjusted"

  def assignment: Assignment = target.entity

  def feedback: Feedback = item.entity

  def recipients: Seq[User] = {
    if (assignment.hasWorkflow) {
      feedback.markerFeedback.asScala.toSeq.sortBy(_.stage.order).headOption.map(_.marker).toSeq
    } else {
      Seq()
    }

  }

  def title = s"${assignment.module.code.toUpperCase} - for ${assignment.name} : Adjustments have been made to feedback for ${feedback.studentIdentifier}"

  def content = FreemarkerModel(FeedbackAdjustmentNotification.templateLocation,
    Map(
      "assignment" -> assignment,
      "feedback" -> feedback
    ))

  def url: String = recipients.headOption.map(recipient => Routes.admin.assignment.markerFeedback(assignment, recipient)).getOrElse("")

  def urlTitle = "mark these submissions"

  priority = Info

}
