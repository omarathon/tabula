package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent


object MitCircsSubmissionReceiptNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_submission_receipt.ftl"

  def render(): FreemarkerModel = FreemarkerModel(templateLocation, Map())
}

@Entity
@DiscriminatorValue("MitCircsSubmissionReceipt")
class MitCircsSubmissionReceiptNotification
  extends NotificationWithTarget[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]
    with SingleRecipientNotification
    with UserIdRecipientNotification
    with MyWarwickActivity
    with AutowiringUserLookupComponent
    with Logging {

  override def onPreSave(isNew: Boolean) {
    recipientUserId = submission.student.userId
  }

  def verb = "received"

  def submission: MitigatingCircumstancesSubmission = target.entity

  def title: String = s"Mitigating circumstances submission received"

  def content: FreemarkerModel = MitCircsSubmissionReceiptNotification.render()

  def url: String = Routes.Student.home(submission.student)

  def urlTitle = s"view your mitigating circumstances submissions"

}

