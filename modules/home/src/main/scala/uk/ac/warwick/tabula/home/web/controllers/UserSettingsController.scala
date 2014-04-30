package uk.ac.warwick.tabula.home.web.controllers

import uk.ac.warwick.spring.Wire
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.home.commands.UserSettingsCommand
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.UserSettingsService
import javax.validation.Valid
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}

@Controller
@RequestMapping(Array("/settings"))
class UserSettingsController extends BaseController {

	validatesSelf[SelfValidating]
	
	hideDeletedItems
	
	var userSettingsService = Wire.auto[UserSettingsService]
	var moduleService = Wire[ModuleAndDepartmentService]
	
	private def getUserSettings(user: CurrentUser) = 
		userSettingsService.getByUserId(user.apparentId) 
		
		
	@ModelAttribute("userSettingsCommand")
	def command(user: CurrentUser) = {
		val usersettings = getUserSettings(user)
		usersettings match { 
			case Some(setting) => UserSettingsCommand(user, setting)
			case None => UserSettingsCommand(user, new UserSettings(user.apparentId))
		}
	}

	
	@RequestMapping(method=Array(GET, HEAD))
	def viewSettings(user: CurrentUser, @ModelAttribute("userSettingsCommand") command: Appliable[Unit], errors:Errors) = {
		Mav("usersettings/form",
			"isCourseworkModuleManager" -> !moduleService.modulesWithPermission(user, Permissions.Module.ManageAssignments).isEmpty,
			"isDepartmentalAdmin" -> !moduleService.departmentsWithPermission(user, Permissions.Module.ManageAssignments).isEmpty
		)
	}

	@RequestMapping(method=Array(POST))
	def saveSettings(@ModelAttribute("userSettingsCommand") @Valid command: Appliable[Unit], errors:Errors) = {
		if (errors.hasErrors){
			viewSettings(user, command, errors)
		}
		else{
			command.apply()
			Redirect("/home")
		}
	}
}
