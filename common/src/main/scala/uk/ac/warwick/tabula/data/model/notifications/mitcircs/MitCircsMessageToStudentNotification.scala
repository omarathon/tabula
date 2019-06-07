package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsMessageToStudentNotification._
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object MitCircsMessageToStudentNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_message_student.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsMessageToStudent")
class MitCircsMessageToStudentNotification extends AbstractMitCircsMessageToStudentNotification

@Entity
@Proxy
@DiscriminatorValue("MitCircsMessageToStudentWithReminder")
class MitCircsMessageToStudentWithReminderNotification extends AbstractMitCircsMessageToStudentNotification
  with RecipientCompletedActionRequiredNotification {

  override def onPreSave(isNew: Boolean): Unit = {
    super.onPreSave(isNew)
    priority = NotificationPriority.Warning
  }
}

abstract class AbstractMitCircsMessageToStudentNotification
  extends NotificationWithTarget[MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission]
    with SingleItemNotification[MitigatingCircumstancesMessage]
    with SingleRecipientNotification
    with UserIdRecipientNotification
    with MyWarwickNotification
    with AutowiringUserLookupComponent
    with Logging {

  override def onPreSave(isNew: Boolean) {
    recipientUserId = submission.student.userId
  }

  def verb = "received"

  def submission: MitigatingCircumstancesSubmission = target.entity

  def replyByDate: Option[String] = item.entity.replyByDate.map(dateTimeFormatter.print)

  def title: String = s"Mitigating circumstances submission MIT-${submission.key} - message received"

  def content: FreemarkerModel = FreemarkerModel(templateLocation, Map("submission" -> submission, "replyByDate" -> replyByDate))

  def url: String = Routes.Profile.PersonalCircumstances.viewMessages(submission)

  def urlTitle = s"view your mitigating circumstances submission"

}

