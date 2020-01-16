package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

/**
  * Keep this so that activity streams containing this notification render properly
  */

object ModeratorRejectedNotification {
  val templateLocation = "/WEB-INF/freemarker/emails/moderator_rejected_notification.ftl"
}

@Deprecated
@Entity
@Proxy
@DiscriminatorValue(value = "ModeratorRejected")
class ModeratorRejectedNotification extends Notification[MarkerFeedback, Unit]
  with SingleItemNotification[MarkerFeedback]
  with AutowiringUserLookupComponent
  with AllCompletedActionRequiredNotification {

  def markerFeedback: MarkerFeedback = item.entity

  def parentFeedback: Feedback = markerFeedback.feedback

  def assignment: Assignment = parentFeedback.assignment

  def verb = "Released"

  def title: String = "%s: Feedback for %s for \"%s\" has been rejected by the moderator"
    .format(assignment.module.code.toUpperCase, parentFeedback.studentIdentifier, assignment.name)

  def content = FreemarkerModel(ModeratorRejectedNotification.templateLocation,
    Map(
      "moderatorName" -> agent.getFullName,
      "studentId" -> parentFeedback.studentIdentifier,
      "assignment" -> assignment,
      "rejectionComments" -> "",
      "adjustedMark" -> markerFeedback.mark,
      "adjustedGrade" -> markerFeedback.grade
    ))

  def url: String = Routes.admin.assignment.submissionsandfeedback.summary(assignment)

  def urlTitle = "update the feedback and submit it for moderation again"

  def recipients: Seq[User] = Seq()
}