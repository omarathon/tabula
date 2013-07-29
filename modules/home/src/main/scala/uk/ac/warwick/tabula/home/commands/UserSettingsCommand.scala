package uk.ac.warwick.tabula.home.commands

import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.data.model.UserSettings.Settings
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.UserSettingsService

class UserSettingsCommand(user: CurrentUser, settings: UserSettings) extends Command[Unit] with SelfValidating  {

	PermissionCheck(Permissions.UserSettings.Update, settings)
	
	var service = Wire.auto[UserSettingsService]
	
	var alertsSubmission = settings.alertsSubmission
	var weekNumberingSystem = settings.weekNumberingSystem
		
	override def applyInternal() = transactional() {
		settings.alertsSubmission = alertsSubmission
		settings.weekNumberingSystem = if (weekNumberingSystem.hasText) weekNumberingSystem else null
		
		service.save(user, settings)
	}
	
	override def describe(d:Description) {
		d.properties("user" -> user.apparentId)
	}	
	
	override def validate(errors:Errors) {
		if (!user.exists){
			errors.reject("user.mustBeLoggedIn")
		}
	}
}