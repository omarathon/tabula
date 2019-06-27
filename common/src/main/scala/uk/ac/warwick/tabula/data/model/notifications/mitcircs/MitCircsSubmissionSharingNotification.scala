package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickNotification, Notification, NotificationWithTarget, SingleItemNotification}
import uk.ac.warwick.userlookup.User
import MitCircsSubmissionSharingNotification._
import uk.ac.warwick.tabula.profiles.web.Routes

object MitCircsSubmissionSharingNotification {
  val sharedTemplateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_shared_submission.ftl"
  val unsharedTemplateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_unshared_submission.ftl"
}

abstract class MitCircsSubmissionSharingNotification
  extends Notification[MitigatingCircumstancesSubmission, Unit]
    with SingleItemNotification[MitigatingCircumstancesSubmission]
    with MyWarwickNotification {

  @transient var modifiedUsers: Seq[User] = Nil

  override def recipients: Seq[User] = modifiedUsers
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsSubmissionAddSharing")
class MitCircsSubmissionAddSharingNotification extends MitCircsSubmissionSharingNotification {
  override def verb: String = "shared"

  override def title: String = s"${agent.getFullName} has shared their mitigating circumstances submission MIT-${item.entity.key}"

  override def content: FreemarkerModel = FreemarkerModel(sharedTemplateLocation, Map(
    "agent" -> agent,
    "submission" -> item.entity,
  ))

  override def url: String = Routes.Profile.PersonalCircumstances.view(item.entity)

  override def urlTitle: String = "view the mitigating circumstances submission"
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsSubmissionRemoveSharing")
class MitCircsSubmissionRemoveSharingNotification extends MitCircsSubmissionSharingNotification {
  override def verb: String = "unshared"

  override def title: String = s"${agent.getFullName} has unshared their mitigating circumstances submission MIT-${item.entity.key}"

  override def content: FreemarkerModel = FreemarkerModel(unsharedTemplateLocation, Map(
    "agent" -> agent,
    "submission" -> item.entity,
  ))

  override def url: String = Routes.Profile.identity(item.entity.student)

  override def urlTitle: String = "view the student's profile"
}