package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.userlookup.User

object PendingEvidenceReminderNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/pending_evidence_reminder.ftl"
}

@Entity
@DiscriminatorValue("PendingEvidenceReminder")
class PendingEvidenceReminderNotification
  extends Notification[MitigatingCircumstancesSubmission, Unit]
    with SingleItemNotification[MitigatingCircumstancesSubmission]
    with AllCompletedActionRequiredNotification
    with SingleRecipientNotification
    with Logging {

  def verb = "upload"

  def submission: MitigatingCircumstancesSubmission = item.entity

  override def recipient: User = submission.student.asSsoUser

  def title: String = s"Mitigating circumstances evidence required for MIT-${submission.key}"

  def content: FreemarkerModel = FreemarkerModel(PendingEvidenceReminderNotification.templateLocation, Map("submission" -> submission))

  def url: String = Routes.Profile.PersonalCircumstances.pendingEvidence(submission)

  def urlTitle = s"upload your pending evidence"

}

