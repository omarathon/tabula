package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.spring.Wire
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.Features
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMethod, RequestMapping, PathVariable}
import scala.Array
import uk.ac.warwick.tabula.coursework.commands.departments.DisplaySettingsCommand
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.commands.UserSettingsCommand
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.UserSettingsService
import javax.validation.Valid

@Controller
@RequestMapping(Array("/admin/usersettings"))
class UserSettingsController extends CourseworkController {

	validatesSelf[UserSettingsCommand]
	
	var userSettingsService = Wire.auto[UserSettingsService]
	
	private def getUserSettings(user: CurrentUser) = 
		userSettingsService.getByUserId(user.apparentId) 
		
		
	@ModelAttribute def command(user: CurrentUser) = {
		val usersettings = getUserSettings(user)
		usersettings match { 
			case Some(setting) => new UserSettingsCommand(user, setting)
			case None => new UserSettingsCommand(user, new UserSettings(user.apparentId))
		}
	}

	
	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def viewSettings(user: CurrentUser, command:UserSettingsCommand, errors:Errors) = {		
		 Mav("admin/user-settings")	 
	}

	@RequestMapping(method=Array(RequestMethod.POST))
	def saveSettings(@Valid command:UserSettingsCommand, errors:Errors) = {
		command.validate(errors)
		if (errors.hasErrors){
			viewSettings(user, command, errors)
		}
		else{
			command.apply()
			Redirect("/../coursework")
		}
	}
}
