package uk.ac.warwick.tabula.commands.home

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.data.model.notifications.coursework.FinaliseFeedbackNotificationSettings
import uk.ac.warwick.tabula.data.model.notifications.groups.reminders.SmallGroupEventAttendanceReminderNotificationSettings
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringUserSettingsServiceComponent, UserSettingsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.concurrent.duration.FiniteDuration

object UserSettingsCommand {
  type Command = Appliable[UserSettings] with SelfValidating
  val RequiredPermission: Permission = Permissions.UserSettings.Update

  def apply(user: CurrentUser, settings: UserSettings): Command =
    new UserSettingsCommand(user, settings)
      with ComposableCommand[UserSettings]
      with UserSettingsRequest
      with UserSettingsPermission
      with UserSettingsCommandValidation
      with UserSettingsDescription
      with UserSettingsCommandState
      with AutowiringUserSettingsServiceComponent
}

abstract class UserSettingsCommand(val user: CurrentUser, val settings: UserSettings) extends CommandInternal[UserSettings] {
  self: UserSettingsRequest
    with UserSettingsServiceComponent =>

  override def applyInternal(): UserSettings = transactional() {
    settings.alertsSubmission = alertsSubmission
    settings.newAssignmentSettings = newAssignmentSettings
    settings.weekNumberingSystem = weekNumberingSystem.maybeText.orNull
    settings.bulkEmailSeparator = bulkEmailSeparator
    settings.batchedNotifications = batchedNotifications

    smallGroupEventAttendanceReminderSettings.enabled.value = smallGroupEventAttendanceReminderEnabled
    finaliseFeedbackNotificationSettings.enabled.value = finaliseFeedbackNotificationEnabled
    settings.deptAdminReceiveStudentComments = deptAdminReceiveStudentComments

    userSettingsService.save(user, settings)
    settings
  }

}

trait UserSettingsRequest {
  self: UserSettingsCommandState =>

  var alertsSubmission: String = settings.alertsSubmission
  var newAssignmentSettings: String = settings.newAssignmentSettings
  var weekNumberingSystem: String = settings.weekNumberingSystem
  var bulkEmailSeparator: String = settings.bulkEmailSeparator
  var deptAdminReceiveStudentComments: Boolean = settings.deptAdminReceiveStudentComments
  var batchedNotifications: FiniteDuration = settings.batchedNotifications

  lazy val smallGroupEventAttendanceReminderSettings = new SmallGroupEventAttendanceReminderNotificationSettings(settings.notificationSettings("SmallGroupEventAttendanceReminder"))
  var smallGroupEventAttendanceReminderEnabled: Boolean = smallGroupEventAttendanceReminderSettings.enabled.value

  lazy val finaliseFeedbackNotificationSettings = new FinaliseFeedbackNotificationSettings(settings.notificationSettings("FinaliseFeedback"))
  var finaliseFeedbackNotificationEnabled: Boolean = finaliseFeedbackNotificationSettings.enabled.value
}

trait UserSettingsCommandValidation extends SelfValidating {
  self: UserSettingsRequest with UserSettingsCommandState =>

  override def validate(errors: Errors): Unit =
    if (!user.exists) {
      errors.reject("user.mustBeLoggedIn")
    }
}

trait UserSettingsDescription extends Describable[UserSettings] {
  self: UserSettingsCommandState =>

  override def describe(d: Description): Unit =
    d.users(Seq(user.apparentUser))

  override def describeResult(d: Description, result: UserSettings): Unit = {
    result.activeDepartment.foreach(d.department)
    d.properties(
      "user" -> user.apparentId,
      "alertsSubmission" -> result.alertsSubmission,
      "newAssignmentSettings" -> result.newAssignmentSettings,
      "hiddenIntros" -> result.hiddenIntros,
      "weekNumberingSystem" -> result.weekNumberingSystem,
      "bulkEmailSeparator" -> result.bulkEmailSeparator,
      "activeAcademicYear" -> result.activeAcademicYear.map(_.toString)
    )
  }
}

trait UserSettingsPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: UserSettingsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(UserSettingsCommand.RequiredPermission, mandatory(settings))
}

trait UserSettingsCommandState {
  def user: CurrentUser
  def settings: UserSettings
}
