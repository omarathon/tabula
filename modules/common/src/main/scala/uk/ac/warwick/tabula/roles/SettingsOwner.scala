package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class SettingsOwner(settings: model.UserSettings) extends BuiltInRole(settings) {
	
	GrantsPermissionFor(settings, 
		UserSettings.Update
	)

}