package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsMessageToStudentNotification.templateLocation
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent


object MitCircsMessageToStudentNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_message_student.ftl"
}

@Entity
@DiscriminatorValue("MitCircsMessageToStudent")
class MitCircsMessageToStudentNotification
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

  def title: String = s"Mitigating circumstances submission MIT-${submission.key} - message received"

  def content: FreemarkerModel = FreemarkerModel(templateLocation, Map("submission" -> submission))

  def url: String = Routes.Profile.PersonalCircumstances.viewMessages(submission)

  def urlTitle = s"view your mitigating circumstances submission"

}

