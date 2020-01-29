package uk.ac.warwick.tabula.web.controllers.home

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PostMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.home.UserSettingsCommand
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.data.model.notifications.coursework.FinaliseFeedbackNotificationSettings
import uk.ac.warwick.tabula.data.model.notifications.groups.reminders.SmallGroupEventAttendanceReminderNotificationSettings
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, ModuleAssistantRoleDefinition, ModuleManagerRoleDefinition}
import uk.ac.warwick.tabula.services.permissions.AutowiringPermissionsServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringSmallGroupServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

import scala.concurrent.duration._

@Controller
class UserSettingsController extends BaseController
  with AutowiringUserSettingsServiceComponent
  with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringSmallGroupServiceComponent
  with AutowiringPermissionsServiceComponent {

  type UserSettingsCommand = UserSettingsCommand.Command

  validatesSelf[SelfValidating]
  hideDeletedItems

  private def getUserSettings(user: CurrentUser): Option[UserSettings] =
    userSettingsService.getByUserId(user.apparentId)

  @ModelAttribute("userSettingsCommand")
  def command(user: CurrentUser): UserSettingsCommand =
    getUserSettings(user) match {
      case Some(setting) => UserSettingsCommand(user, setting)
      case None => UserSettingsCommand(user, new UserSettings(user.apparentId))
    }

  @ModelAttribute("batchedNotificationSettings")
  def batchedNotificationSettings: Seq[FiniteDuration] = Seq(
    Duration.Zero,
    5.minutes,
    10.minutes,
    30.minutes,
    1.hour
  )

  @RequestMapping(Array("/settings"))
  def viewSettings(user: CurrentUser, @ModelAttribute("userSettingsCommand") command: UserSettingsCommand, errors: Errors, success: Boolean = false): Mav = {
    val deptsUserIsAdminOn = moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.ManageAssignments)
    val mustNotBeCurrentUser: User => Boolean = u => u.getUserId != user.userId

    val deptsWithNoOtherContacts = deptsUserIsAdminOn.map { department =>
      val otherAdmins = department.owners.users.filter(mustNotBeCurrentUser)

      val hasAtLeastOneOtherContact = otherAdmins.exists { u =>
        u.isFoundUser &&
        u.getEmail.hasText &&
        userSettingsService.getByUserId(u.getUserId).forall(_.deptAdminReceiveStudentComments)
      }

      department.name -> hasAtLeastOneOtherContact
    }.filter { case (_, hasAtLeastOneOtherContact) => !hasAtLeastOneOtherContact }.map { case (departmentName, _) => departmentName }

    lazy val allDepartments = moduleAndDepartmentService.allDepartments
    lazy val grantedRoles = permissionsService.getAllGrantedRolesFor(user)

    val canReceiveSmallGroupAttendanceReminders: Boolean = (
      // Is a named user on a department's notification settings
      allDepartments.exists(d => new SmallGroupEventAttendanceReminderNotificationSettings(d.notificationSettings("SmallGroupEventAttendanceReminder")).namedUsers.value.contains(user.apparentUser)) ||

      // Is a tutor on a small group event
      smallGroupService.findSmallGroupEventsByTutor(user.apparentUser).nonEmpty ||

      // Is a module assistant
      grantedRoles.exists(_.roleDefinition == ModuleAssistantRoleDefinition) ||

      // Is a module manager
      grantedRoles.exists(_.roleDefinition == ModuleManagerRoleDefinition) ||

      // Is a departmental administrator
      grantedRoles.exists(_.roleDefinition == DepartmentalAdministratorRoleDefinition)
    )

    val canReceiveFinaliseFeedbackNotifications: Boolean = (
      // Is a named user on a department's notification settings
      allDepartments.exists(d => new FinaliseFeedbackNotificationSettings(d.notificationSettings("FinaliseFeedback")).namedUsers.value.contains(user.apparentUser)) ||

      // Is a module manager
      grantedRoles.exists(_.roleDefinition == ModuleManagerRoleDefinition) ||

      // Is a departmental administrator
      grantedRoles.exists(_.roleDefinition == DepartmentalAdministratorRoleDefinition)
    )

    Mav("usersettings/form",
      "isCourseworkModuleManager" -> moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments).nonEmpty,
      "isDepartmentalAdmin" -> deptsUserIsAdminOn.nonEmpty,
      "deptsWithNoOtherContacts" -> deptsWithNoOtherContacts,
      "canReceiveSmallGroupAttendanceReminders" -> canReceiveSmallGroupAttendanceReminders,
      "canReceiveFinaliseFeedbackNotifications" -> canReceiveFinaliseFeedbackNotifications,
      "success" -> success
    )
  }

  @PostMapping(Array("/settings"))
  def saveSettings(@ModelAttribute("userSettingsCommand") @Valid command: UserSettingsCommand, errors: Errors): Mav =
    if (errors.hasErrors) {
      viewSettings(user, command, errors)
    } else {
      command.apply()
      viewSettings(user, command, errors, success = true)
    }

  @RequestMapping(Array("/settings.json"))
  def viewSettingsJson(user: CurrentUser): Mav = {
    val usersettings =
      getUserSettings(user) match {
        case Some(setting) => JSONUserSettings(setting)
        case None => JSONUserSettings(new UserSettings(user.apparentId))
      }

    Mav(new JSONView(usersettings))
  }

  @PostMapping(Array("/settings.json"))
  def saveSettingsJson(@ModelAttribute("userSettingsCommand") @Valid command: UserSettingsCommand, errors: Errors): Mav = {
    if (!errors.hasErrors) command.apply()

    viewSettingsJson(user)
  }
}

case class JSONUserSettings(
  alertsSubmission: String,
  newAssignmentSettings: String,
  weekNumberingSystem: String,
  bulkEmailSeparator: String,
)

object JSONUserSettings {
  def apply(u: UserSettings): JSONUserSettings = {
    JSONUserSettings(
      alertsSubmission = u.alertsSubmission,
      newAssignmentSettings = u.newAssignmentSettings,
      weekNumberingSystem = u.weekNumberingSystem,
      bulkEmailSeparator = u.bulkEmailSeparator,
    )
  }
}
