package uk.ac.warwick.tabula.coursework.commands

import scala.reflect.BeanProperty

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
	
	@BeanProperty var alertsSubmission = settings.alertsSubmission
		
	override def applyInternal() {
		transactional() {
			settings.alertsSubmission = alertsSubmission
			service.save(user, settings)
		}
	}
	
	override def describe(d:Description) {
		d.properties("user" -> user.apparentId)
	}	
	
	override def validate(errors:Errors) {
		if (!user.exists){
			errors.rejectValue("","user.mustBeLoggedIn")
		}
	}
}