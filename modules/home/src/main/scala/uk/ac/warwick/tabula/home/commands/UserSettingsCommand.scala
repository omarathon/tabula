package uk.ac.warwick.tabula.home.commands

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, Describable, ComposableCommand, Description, SelfValidating}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringUserSettingsServiceComponent, UserSettingsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object UserSettingsCommand {

	def apply(user: CurrentUser, settings: UserSettings) =
		new UserSettingsCommand(user, settings)
		with ComposableCommand[Unit]
		with UserSettingsPermission
		with UserSettingsCommandValidation
		with UserSettingsDescription
		with UserSettingsCommandState
		with AutowiringUserSettingsServiceComponent
}

class UserSettingsCommand(val user: CurrentUser, val settings: UserSettings) extends CommandInternal[Unit] {

	self: UserSettingsServiceComponent =>
	
	var alertsSubmission = settings.alertsSubmission
	var weekNumberingSystem = settings.weekNumberingSystem
	var bulkEmailSeparator = settings.bulkEmailSeparator
	var profilesDefaultView = settings.profilesDefaultView
		
	override def applyInternal() = transactional() {
		settings.alertsSubmission = alertsSubmission
		settings.weekNumberingSystem = if (weekNumberingSystem.hasText) weekNumberingSystem else null
		settings.bulkEmailSeparator = bulkEmailSeparator
		settings.profilesDefaultView = profilesDefaultView

		userSettingsService.save(user, settings)
	}

}

trait UserSettingsCommandValidation extends SelfValidating {

	self: UserSettingsCommandState =>

	override def validate(errors:Errors) {
		if (!user.exists) {
			errors.reject("user.mustBeLoggedIn")
		}
	}

}

trait UserSettingsDescription extends Describable[Unit] {

	self: UserSettingsCommandState =>

	override def describe(d:Description) {
		d.properties("user" -> user.apparentId)
	}

}

trait UserSettingsPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: UserSettingsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.UserSettings.Update, settings)
	}

}

trait UserSettingsCommandState {

	def user: CurrentUser
	def settings: UserSettings

}