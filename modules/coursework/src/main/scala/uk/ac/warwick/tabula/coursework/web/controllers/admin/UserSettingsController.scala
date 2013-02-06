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


@Controller
@RequestMapping(Array("/admin/usersettings"))
class UserSettingsController extends CourseworkController {

	var userSettingsService = Wire.auto[UserSettingsService]
	
	private def getUserSettings(user: CurrentUser) = 
		userSettingsService.getByUserId(user.apparentId) 
		
	@ModelAttribute def formOrNull(user: CurrentUser) = {
		val usersettings = getUserSettings(user)
		usersettings match { 
			case Some(setting) => new UserSettingsCommand(user, setting)
			case None => new UserSettingsCommand(user, new UserSettings(user.apparentId))
		}
	}

	
	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def viewSettings(user: CurrentUser, formOrNull:UserSettingsCommand, errors:Errors) = {
		if(!errors.hasErrors){
			formOrNull.copySettings()
		}
		
		 Mav("admin/user-settings")	 
	}

	@RequestMapping(method=Array(RequestMethod.POST))
	def saveSettings(formOrNull:UserSettingsCommand, errors:Errors) = {
		formOrNull.validate(errors)
		if (errors.hasErrors){
			viewSettings(user, formOrNull, errors)
		}
		else{
			formOrNull.apply()
			Redirect("/../coursework")
		}
	}
}
