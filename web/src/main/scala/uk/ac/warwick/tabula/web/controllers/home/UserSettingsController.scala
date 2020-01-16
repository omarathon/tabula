package uk.ac.warwick.tabula.web.controllers.home

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PostMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.home.UserSettingsCommand
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

import scala.concurrent.duration._

@Controller
class UserSettingsController extends BaseController
  with AutowiringUserSettingsServiceComponent
  with AutowiringModuleAndDepartmentServiceComponent {

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

    val deptsWithNoOtherContacts = deptsUserIsAdminOn.map(d => {
      (d.name, d.owners.users.filter(mustNotBeCurrentUser))
    }).map { case (departmentName, otherAdmins) =>
      (
        departmentName,
        otherAdmins.map(u => userSettingsService
          .getByUserId(u.getUserId)
          .map(_.deptAdminReceiveStudentComments))
          .map(_.getOrElse(true)).exists(identity[Boolean])
      )
    }.filter { case (_, hasAtLeastOneOtherContact) => !hasAtLeastOneOtherContact }.map { case (departmentName, _) => departmentName }

    Mav("usersettings/form",
      "isCourseworkModuleManager" -> moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments).nonEmpty,
      "isDepartmentalAdmin" -> deptsUserIsAdminOn.nonEmpty,
      "deptsWithNoOtherContacts" -> deptsWithNoOtherContacts,
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
