package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent


object MitCircsSubmissionUpdatedNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_submission_updated.ftl"
}

@Entity
@DiscriminatorValue("MitCircsSubmissionUpdated")
class MitCircsSubmissionUpdatedNotification
  extends NotificationWithTarget[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]
    with NotifiesMitCircsOfficers
    with MyWarwickNotification
    with AutowiringUserLookupComponent
    with Logging {

  def verb = "updated"

  def submission: MitigatingCircumstancesSubmission = target.entity

  def title: String = s"Mitigating circumstances submission MIT-${submission.key} updated"

  def content: FreemarkerModel = FreemarkerModel(MitCircsSubmissionUpdatedNotification.templateLocation, Map("submission" -> submission))

  def url: String = Routes.Admin.review(submission)

  def urlTitle = s"view this mitigating circumstances submission"

}

