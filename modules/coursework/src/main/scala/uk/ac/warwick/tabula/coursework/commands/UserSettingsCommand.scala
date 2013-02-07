package uk.ac.warwick.tabula.coursework.commands
import scala.reflect.BeanProperty
import uk.ac.warwick.spring.Wire
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.permissions.Permissions


class UserSettingsCommand(user: CurrentUser, usersettings: UserSettings) extends Command[Unit] with Public with SelfValidating  {

	PermissionCheck(Permissions.UserSettings.Update)
	
	var service = Wire.auto[UserSettingsService]
	@BeanProperty var alertsSubmission : String =_
	
	copySettings()
		
	def validate(errors:Errors){
		
	}
	
	def copySettings() {
		var settingsOpt = service.parseJson(usersettings.data)
		for(settings <- settingsOpt) {
			alertsSubmission = settings.getOrElse("alertsSubmission", "").toString
		}
	}

	override def applyInternal() {
		transactional() {
			usersettings.fieldsMap = Map("alertsSubmission" -> alertsSubmission)
			service.save(user, usersettings)
		}
	}
	
	// describe the thing that's happening.
	override def describe(d:Description) {
		//d.properties("department" -> department.code)
	}
}