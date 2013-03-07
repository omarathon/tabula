package uk.ac.warwick.tabula.home.web.controllers;

import scala.collection.JavaConversions._

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping

import javax.validation.Valid
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.home.commands.UserSettingsCommand
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(value = Array("/settings/showIntro/{page}"))
class IntroUserSettingsController extends HomeController {
	validatesSelf[UserSettingsCommand]
	
	var userSettingsService = Wire.auto[UserSettingsService]
	
	private def getUserSettings(user: CurrentUser) = userSettingsService.getByUserId(user.apparentId)
	
	@ModelAttribute def command(user: CurrentUser) = {
		val usersettings = getUserSettings(user)
		usersettings match { 
			case Some(setting) => new UserSettingsCommand(user, setting)
			case None => new UserSettingsCommand(user, new UserSettings(user.apparentId))
		}
	}
		
	@RequestMapping(method=Array(GET, HEAD))
	def readSettings(user: CurrentUser) = {
		Mav(new JSONView(false))
	}

	@RequestMapping(method=Array(POST))
	def saveSettings(@Valid command: UserSettingsCommand) = {
		command.apply()
	}
}