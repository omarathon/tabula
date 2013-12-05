package uk.ac.warwick.tabula.home.web.controllers

import uk.ac.warwick.spring.Wire
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.home.commands.UserSettingsCommand
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.UserSettingsService
import javax.validation.Valid
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.tabula.data.ModuleDao
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/settings"))
class UserSettingsController extends BaseController {

	validatesSelf[UserSettingsCommand]
	
	hideDeletedItems
	
	var userSettingsService = Wire.auto[UserSettingsService]
	var moduleService = Wire[ModuleAndDepartmentService]
	
	private def getUserSettings(user: CurrentUser) = 
		userSettingsService.getByUserId(user.apparentId) 
		
		
	@ModelAttribute def command(user: CurrentUser) = {
		val usersettings = getUserSettings(user)
		usersettings match { 
			case Some(setting) => new UserSettingsCommand(user, setting)
			case None => new UserSettingsCommand(user, new UserSettings(user.apparentId))
		}
	}

	
	@RequestMapping(method=Array(GET, HEAD))
	def viewSettings(user: CurrentUser, command:UserSettingsCommand, errors:Errors) = {
		 Mav("usersettings/form", "isCourseworkModuleManager" -> !moduleService.modulesWithPermission(user, Permissions.Module.ManageAssignments).isEmpty)
	}

	@RequestMapping(method=Array(POST))
	def saveSettings(@Valid command:UserSettingsCommand, errors:Errors) = {
		if (errors.hasErrors){
			viewSettings(user, command, errors)
		}
		else{
			command.apply()
			Redirect("/home")
		}
	}
}
