package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.HasSettings.{BooleanSetting, UserSeqSetting}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesAcuteOutcome, MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsRecordAcuteOutcomesNotification._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmissionState.{ApprovedByChair, OutcomesRecorded}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object MitCircsRecordAcuteOutcomesNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_acute_outcomes.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsRecordAcuteOutcomes")
class MitCircsRecordAcuteOutcomesNotification
  extends Notification[MitigatingCircumstancesSubmission, Unit]
    with SingleItemNotification[MitigatingCircumstancesSubmission]
    with MyWarwickNotification
    with ConfigurableNotification {

  @transient private lazy val submission = item.entity
  @transient private lazy val student = submission.student

  override def verb: String = "recorded"

  override def title: String = s"${agent.getFullName} has recorded acute mitigating circumstances outcomes for ${student.fullName.getOrElse(student.universityId)}, MIT-${submission.key}"

  override def content: FreemarkerModel = FreemarkerModel(templateLocation, Map(
    "agent" -> agent,
    "submission" -> submission,
    "student" -> student,
    "outcomesLastRecordedOn" -> Option(submission.outcomesLastRecordedOn).map(dateTimeFormatter.print),
    "startDate" -> dateOnlyFormatter.print(submission.startDate),
    "endDate" -> Option(submission.endDate).map(dateOnlyFormatter.print),
  ))

  override def url: String = Routes.Profile.PersonalCircumstances(item.entity.student)

  override def urlTitle: String = "view the student's personal circumstances"

  @transient
  final lazy val configuringDepartment: Department =
    Option(student.mostSignificantCourse).flatMap(c => Option(c.department)).flatMap(_.subDepartmentsContaining(student).filter(_.enableMitCircs)).head

  override def allRecipients: Seq[User] = {
    if (submission.state != MitigatingCircumstancesSubmissionState.OutcomesRecorded || !submission.isAcute || (Option(submission.acuteOutcome).isEmpty && submission.affectedAssessments.asScala.forall(a => Option(a.acuteOutcome).isEmpty))) {
      Nil
    } else {
      var users: Seq[User] = Seq()

      val settings = new MitCircsRecordAcuteOutcomesNotificationSettings(departmentSettings)
      val notifyAllGroups = !settings.notifyFirstNonEmptyGroupOnly.value

      if (settings.notifyNamedUsers.value && settings.notifyNamedUsersFirst.value) {
        users ++= settings.namedUsers.value
      }

      // Extension notifications go to extension managers too
      if (settings.notifyExtensionManagers.value && (users.isEmpty || notifyAllGroups)) {
        users ++= submission.affectedAssessments.asScala
          .filter(_.acuteOutcome == MitigatingCircumstancesAcuteOutcome.Extension)
          .map(_.module.adminDepartment)
          .distinct
          .flatMap(_.extensionManagers.users)
          .distinct
          .filterNot(_ == agent)
      }

      if (settings.notifyDepartmentAdministrators.value && (users.isEmpty || notifyAllGroups)) {
        users ++= submission.affectedAssessments.asScala
          .filter(a => Option(a.acuteOutcome).isDefined)
          .flatMap(a => Option(a.module))
          .map(_.adminDepartment)
          .distinct
          .flatMap(_.owners.users)
          .distinct
          .filterNot(_ == agent)
      }

      if (settings.notifyNamedUsers.value && !settings.notifyNamedUsersFirst.value && (users.isEmpty || notifyAllGroups)) {
        users ++= settings.namedUsers.value
      }

      // Don't notify the person who did the action
      users.distinct.filterNot(_ == agent)
    }
  }

}

class MitCircsRecordAcuteOutcomesNotificationSettings(departmentSettings: NotificationSettings) {
  @transient private val userLookup = Wire[UserLookupService]

  // Configuration settings specific to this type of notification
  def enabled: BooleanSetting = departmentSettings.enabled

  def notifyExtensionManagers: BooleanSetting = departmentSettings.BooleanSetting("notifyExtensionManagers", default = true)

  def notifyDepartmentAdministrators: BooleanSetting = departmentSettings.BooleanSetting("notifyDepartmentAdministrators", default = true)

  def notifyNamedUsers: BooleanSetting = departmentSettings.BooleanSetting("notifyNamedUsers", default = false)

  def notifyNamedUsersFirst: BooleanSetting = departmentSettings.BooleanSetting("notifyNamedUsersFirst", default = false)

  def namedUsers: UserSeqSetting = departmentSettings.UserSeqSetting("namedUsers", default = Seq(), userLookup)

  def notifyFirstNonEmptyGroupOnly: BooleanSetting = departmentSettings.BooleanSetting("notifyFirstNonEmptyGroupOnly", default = true)
}
