package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.userlookup.User

object DraftSubmissionReminderNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/draft_submission_reminder.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("DraftSubmissionReminder")
class DraftSubmissionReminderNotification
  extends Notification[MitigatingCircumstancesSubmission, Unit]
    with SingleItemNotification[MitigatingCircumstancesSubmission]
    with AllCompletedActionRequiredNotification
    with SingleRecipientNotification
    with Logging {

  def verb = "submit"

  def submission: MitigatingCircumstancesSubmission = item.entity

  override def recipient: User = submission.student.asSsoUser

  def title: String = s"Mitigating circumstances submission MIT-${submission.key} is awaiting submission"

  def content: FreemarkerModel = FreemarkerModel(DraftSubmissionReminderNotification.templateLocation, Map("submission" -> submission))

  def url: String = Routes.Profile.PersonalCircumstances.view(submission)

  def urlTitle = "submit or withdraw your mitigating circumstances submission"

}

