package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsAwaitingStudentMessageReminderNotification._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object MitCircsAwaitingStudentMessageReminderNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_message_student_reminder.ftl"
}

@Entity
@Proxy(`lazy` = false)
@DiscriminatorValue("MitCircsAwaitingStudentMessageReminder")
class MitCircsAwaitingStudentMessageReminderNotification
  extends Notification[MitigatingCircumstancesSubmission, Unit]
    with SingleItemNotification[MitigatingCircumstancesSubmission]
    with RecipientCompletedActionRequiredNotification
    with SingleRecipientNotification
    with UserIdRecipientNotification
    with AutowiringUserLookupComponent {

  override def onPreSave(isNew: Boolean): Unit = {
    recipientUserId = submission.student.userId
    priority = NotificationPriority.Warning
  }

  def submission: MitigatingCircumstancesSubmission = item.entity

  override def verb: String = "remind"

  override def title: String = s"Mitigating circumstances submission MIT-${submission.key} - response required"

  override def content: FreemarkerModel = FreemarkerModel(templateLocation, Map("submission" -> submission))

  override def url: String = Routes.Profile.PersonalCircumstances.viewMessages(submission)

  override def urlTitle: String = "view your mitigating circumstances submission"
}
