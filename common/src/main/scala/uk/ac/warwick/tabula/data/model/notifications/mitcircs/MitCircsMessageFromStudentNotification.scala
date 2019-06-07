package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsMessageFromStudentNotification.templateLocation
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent


object MitCircsMessageFromStudentNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_message_staff.ftl"
}

@Entity
@Proxy(`lazy` = false)
@DiscriminatorValue("MitCircsMessageFromStudent")
class MitCircsMessageFromStudentNotification
  extends NotificationWithTarget[MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission]
    with SingleItemNotification[MitigatingCircumstancesMessage]
    with NotifiesMitCircsOfficers
    with MyWarwickNotification
    with AutowiringUserLookupComponent
    with Logging {


  def verb = "received"

  def submission: MitigatingCircumstancesSubmission = target.entity

  def title: String = s"Mitigating circumstances submission MIT-${submission.key} - message received"

  def content: FreemarkerModel = FreemarkerModel(templateLocation, Map("submission" -> submission))

  def url: String = Routes.Profile.PersonalCircumstances.viewMessages(submission)

  def urlTitle = s"view the mitigating circumstances submission"

}

