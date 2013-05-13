package uk.ac.warwick.tabula.home.web.controllers

import uk.ac.warwick.spring.Wire
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.home.commands.DismissHiddenIntroCommand
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.UserSettingsService
import javax.validation.Valid
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.tabula.data.ModuleDao
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.views.JSONErrorView
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PathVariable

@Controller
@RequestMapping(Array("/settings/showIntro/{settingHash}"))
class DismissHiddenIntroController extends HomeController {

	validatesSelf[DismissHiddenIntroCommand]
	
	var userSettingsService = Wire.auto[UserSettingsService]
	
	private def getUserSettings(user: CurrentUser) = 
		userSettingsService.getByUserId(user.apparentId) 
		
	@ModelAttribute def command(user: CurrentUser, @PathVariable("settingHash") hash: String) = {	
		val usersettings = getUserSettings(user)
		usersettings match { 
			case Some(setting) => new DismissHiddenIntroCommand(user, setting, hash)
			case None => new DismissHiddenIntroCommand(user, new UserSettings(user.apparentId), hash)
		}
	}

	
	@RequestMapping(method=Array(GET, HEAD))
	def viewSettings(user: CurrentUser, command: DismissHiddenIntroCommand, errors:Errors) = {		
		 Mav(new JSONView(Map("dismiss" -> command.dismiss)))
	}

	@RequestMapping(method=Array(POST))
	def saveSettings(@Valid command: DismissHiddenIntroCommand, errors:Errors) = {
		if (errors.hasErrors){
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()
			Mav(new JSONView(Map("status" -> "success")))
		}
	}
}
