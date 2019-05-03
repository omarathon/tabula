package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object MitCircsSubmissionOnBehalfNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_submission_on_behalf.ftl"
}

abstract class MitCircsOnBehalfNotification
  extends NotificationWithTarget[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]
    with SingleRecipientNotification
    with UserIdRecipientNotification
    with MyWarwickActivity
    with AutowiringUserLookupComponent
    with Logging {

  override def onPreSave(isNew: Boolean) {
    recipientUserId = submission.student.userId
  }

  def action: String

  def verb = "received"

  def submission: MitigatingCircumstancesSubmission = target.entity

  def title: String = s"Mitigating circumstances submission $action on your behalf"

  def content: FreemarkerModel = FreemarkerModel(MitCircsSubmissionOnBehalfNotification.templateLocation, Map("action" -> action))

  def url: String = Routes.Profile.PersonalCircumstances.view(submission)

  def urlTitle = s"review your mitigating circumstances submission"

}

@Entity
@DiscriminatorValue("MitCircsSubmissionOnBehalf")
class MitCircsSubmissionOnBehalfNotification extends MitCircsOnBehalfNotification {
  def action: String = "created"
}

@Entity
@DiscriminatorValue("MitCircsUpdateOnBehalf")
class MitCircsUpdateOnBehalfNotification extends MitCircsOnBehalfNotification {
  def action: String = "updated"
}
