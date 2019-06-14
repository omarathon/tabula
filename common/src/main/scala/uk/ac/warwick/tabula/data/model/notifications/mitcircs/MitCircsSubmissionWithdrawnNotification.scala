package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object MitCircsSubmissionWithdrawnNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_submission_withdrawn.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsSubmissionWithdrawn")
class MitCircsSubmissionWithdrawnNotification
  extends NotificationWithTarget[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]
    with NotifiesMitCircsOfficers
    with MyWarwickNotification
    with AutowiringUserLookupComponent
    with Logging {

  def verb = "withdrawn"

  def submission: MitigatingCircumstancesSubmission = target.entity

  def title: String = s"Mitigating circumstances submission MIT-${submission.key} withdrawn"

  def content: FreemarkerModel = FreemarkerModel(MitCircsSubmissionWithdrawnNotification.templateLocation, Map("submission" -> submission))

  def url: String = Routes.Admin.review(submission)

  def urlTitle = s"view this mitigating circumstances submission"

}

