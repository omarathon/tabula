package uk.ac.warwick.tabula.web.controllers.home

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.home.UserSettingsCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, UserSettingsService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

@Controller
class UserSettingsController extends BaseController {

  type UserSettingsCommand = Appliable[UserSettings]

  validatesSelf[SelfValidating]

  hideDeletedItems

  var userSettingsService: UserSettingsService = Wire.auto[UserSettingsService]
  var moduleService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

  private def getUserSettings(user: CurrentUser) =
    userSettingsService.getByUserId(user.apparentId)


  @ModelAttribute("userSettingsCommand")
  def command(user: CurrentUser): UserSettingsCommand = {
    val usersettings = getUserSettings(user)
    usersettings match {
      case Some(setting) => UserSettingsCommand(user, setting)
      case None => UserSettingsCommand(user, new UserSettings(user.apparentId))
    }
  }

  @RequestMapping(value = Array("/settings"), method = Array(GET, HEAD))
  def viewSettings(user: CurrentUser, @ModelAttribute("userSettingsCommand") command: UserSettingsCommand, errors: Errors, success: Boolean = false): Mav = {
    val deptsUserIsAdminOn = moduleService.departmentsWithPermission(user, Permissions.Module.ManageAssignments)
    val mustNotBeCurrentUser: User => Boolean = u => u.getUserId != user.userId

    val deptsWithNoOtherContacts = deptsUserIsAdminOn.map(d => {
      (d.name, d.owners.users.filter(mustNotBeCurrentUser))
    }).map(tuple => {
      (
        tuple._1,
        tuple._2.map(u => userSettingsService
          .getByUserId(u.getUserId)
          .map(_.deptAdminReceiveStudentComments))
          .map(_.getOrElse(true)).exists(identity[Boolean])
      )
    }).filter(!_._2).map(_._1)

    Mav("usersettings/form",
      "isCourseworkModuleManager" -> moduleService.modulesWithPermission(user, Permissions.Module.ManageAssignments).nonEmpty,
      "isDepartmentalAdmin" -> deptsUserIsAdminOn.nonEmpty,
      "deptsWithNoOtherContacts" -> deptsWithNoOtherContacts,
      "success" -> success
    )
  }

  @RequestMapping(value = Array("/settings"), method = Array(POST))
  def saveSettings(@ModelAttribute("userSettingsCommand") @Valid command: UserSettingsCommand, errors: Errors): Mav = {
    if (errors.hasErrors) {
      viewSettings(user, command, errors)
    }
    else {
      command.apply()
      viewSettings(user, command, errors, success = true)
    }
  }

  @RequestMapping(value = Array("/settings.json"), method = Array(GET, HEAD))
  def viewSettingsJson(user: CurrentUser): Mav = {
    val usersettings =
      getUserSettings(user) match {
        case Some(setting) => JSONUserSettings(setting)
        case None => JSONUserSettings(new UserSettings(user.apparentId))
      }

    Mav(new JSONView(usersettings))
  }

  @RequestMapping(value = Array("/settings.json"), method = Array(POST))
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
  profilesDefaultView: String
)

object JSONUserSettings {
  def apply(u: UserSettings): JSONUserSettings = {
    JSONUserSettings(
      alertsSubmission = u.alertsSubmission,
      newAssignmentSettings = u.newAssignmentSettings,
      weekNumberingSystem = u.weekNumberingSystem,
      bulkEmailSeparator = u.bulkEmailSeparator,
      profilesDefaultView = u.profilesDefaultView
    )
  }
}
