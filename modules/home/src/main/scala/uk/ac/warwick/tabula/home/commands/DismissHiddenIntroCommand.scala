package uk.ac.warwick.tabula.home.commands

import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.Transactions._
import org.hibernate.validator.constraints.NotEmpty
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.spring.Wire
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.Description

class DismissHiddenIntroCommand(user: CurrentUser, settings: UserSettings, settingHash: String) extends Command[Unit] with SelfValidating {
	
	PermissionCheck(Permissions.UserSettings.Update, settings)
	
	var service = Wire[UserSettingsService]	
	var dismiss: Boolean = settings.hiddenIntros.contains(settingHash)
	
	override def applyInternal() = transactional() {
		if (dismiss && !settings.hiddenIntros.contains(settingHash)) settings.hiddenIntros = (settings.hiddenIntros :+ settingHash)
		else if (!dismiss) settings.hiddenIntros = settings.hiddenIntros.filterNot(_ == settingHash)
		
		service.save(user, settings)
	}
	
	override def describe(d:Description) {
		d.properties("user" -> user.apparentId, "hash" -> settingHash)
	}	
	
	override def validate(errors:Errors) {
		if (!user.exists){
			errors.reject("user.mustBeLoggedIn")
		}
	}

}