package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickNotification, NotificationWithTarget}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object MitCircsPendingEvidenceReceivedNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_pending_evidence_received.ftl"
}

@Entity
@Proxy(`lazy` = false)
@DiscriminatorValue("MitCircsPendingEvidenceReceived")
class MitCircsPendingEvidenceReceivedNotification
  extends NotificationWithTarget[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]
    with NotifiesMitCircsOfficers
    with MyWarwickNotification
    with AutowiringUserLookupComponent
    with Logging {

  def verb = "received"

  def submission: MitigatingCircumstancesSubmission = target.entity

  def title: String = s"Pending evidence received for mitigating circumstances submission MIT-${submission.key}"

  def content: FreemarkerModel = FreemarkerModel(MitCircsPendingEvidenceReceivedNotification.templateLocation, Map("submission" -> submission))

  def url: String = Routes.Admin.review(submission)

  def urlTitle = s"view this mitigating circumstances submission"

}
