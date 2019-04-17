package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object NewMitCircsSubmissionNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/new_mit_circs_submission.ftl"
}

@Entity
@DiscriminatorValue("NewMitCircsSubmission")
class NewMitCircsSubmissionNotification
  extends NotificationWithTarget[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]
    with NotifiesMitCircsOfficers
    with MyWarwickActivity
    with AutowiringUserLookupComponent
    with Logging {

  def verb = "submitted"

  def submission: MitigatingCircumstancesSubmission = target.entity

  def title: String = s"New mitigating circumstances submission received"

  def content: FreemarkerModel = FreemarkerModel(NewMitCircsSubmissionNotification.templateLocation, Map("submission" -> submission))

  def url: String = Routes.Admin.review(submission)

  def urlTitle = s"view this mitigating circumstances submission"

}

