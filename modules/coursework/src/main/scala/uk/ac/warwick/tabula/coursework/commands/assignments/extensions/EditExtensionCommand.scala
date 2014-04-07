package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import uk.ac.warwick.tabula.commands.{SchedulesNotifications, Notifies, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Notification}
import uk.ac.warwick.tabula.data.model.notifications.{ExtensionGrantedNotification, ExtensionRequestRespondedRejectNotification, ExtensionRequestRespondedApproveNotification, ExtensionRequestRejectedNotification, ExtensionRequestApprovedNotification, ExtensionChangedNotification}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.data.Transactions._

object EditExtensionCommand {
	def apply(mod: Module, ass: Assignment, uniId: String, sub: CurrentUser, action: String) =
		new EditExtensionCommandInternal(mod, ass, uniId, sub, action)
			with ComposableCommand[Extension]
			with EditExtensionCommandPermissions
			with ModifyExtensionCommandDescription
			with ModifyExtensionCommandValidation
			with EditExtensionCommandNotification
			with AutowiringUserLookupComponent
			with HibernateExtensionPersistenceComponent
}

class EditExtensionCommandInternal(mod: Module, ass: Assignment, uniId: String, sub: CurrentUser, action: String)
		extends ModifyExtensionCommand(mod, ass, uniId, sub, action) with ModifyExtensionCommandState  {
	self: ExtensionPersistenceComponent with UserLookupComponent =>

	val e = assignment.findExtension(universityId)
	e match {
		case Some(e) =>
			copyFrom(e)
			extension = e
		case None => extension = new Extension(universityId)
	}

	def applyInternal() = transactional() {
		copyTo(extension)
		save(extension)
		extension
	}
}


trait EditExtensionCommandPermissions extends RequiresPermissionsChecking {
	self: ModifyExtensionCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)

		extension match {
			case e if e.isTransient => p.PermissionCheck(Permissions.Extension.Create, assignment)
			case _ => p.PermissionCheck(Permissions.Extension.Update, assignment)
		}
	}
}


trait EditExtensionCommandNotification extends Notifies[Extension, Option[Extension]] {
	self: ModifyExtensionCommandState =>

	def emit(extension: Extension) = {
		val admin = submitter.apparentUser

		if (extension.isManual) {
			Seq(Notification.init(new ExtensionGrantedNotification, submitter.apparentUser, Seq(extension), assignment))
		} else {
			val baseNotification = if (extension.isManual) {
				new ExtensionChangedNotification
			} else if (extension.approved) {
				new ExtensionRequestApprovedNotification
			} else {
				new ExtensionRequestRejectedNotification
			}
			val studentNotification = Notification.init(baseNotification, admin, Seq(extension), assignment)

			val baseAdminNotification = if (extension.approved) {
				new ExtensionRequestRespondedApproveNotification
			} else {
				new ExtensionRequestRespondedRejectNotification
			}
			val adminNotifications = Notification.init(baseAdminNotification, admin, Seq(extension), assignment)

			Seq(studentNotification, adminNotifications)
		}
	}
}

trait EditExtensionCommandScheduledNotification extends SchedulesNotifications[Extension] {
	self: ModifyExtensionCommandState =>

}
