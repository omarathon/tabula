package uk.ac.warwick.tabula.commands.coursework.assignments.extensions

import uk.ac.warwick.tabula.data.model.notifications.coursework.ExtensionRevokedNotification

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.{Notifies, Description, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Notification}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.forms.{ExtensionState, Extension}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}

object DeleteExtensionCommand {
	def apply(mod: Module, ass: Assignment, uniId: String, sub: CurrentUser) =
		new DeleteExtensionCommandInternal(mod, ass, uniId, sub)
			with ComposableCommand[Extension]
			with DeleteExtensionCommandPermissions
			with ModifyExtensionCommandDescription
			with DeleteExtensionCommandNotification
			with AutowiringUserLookupComponent
			with HibernateExtensionPersistenceComponent
}

class DeleteExtensionCommandInternal(mod: Module, ass: Assignment, uniId: String, sub: CurrentUser)
	extends ModifyExtensionCommand(mod, ass, uniId, sub) with ModifyExtensionCommandState {

	self: ExtensionPersistenceComponent with UserLookupComponent =>

	extension = assignment.findExtension(universityId).getOrElse({ throw new IllegalStateException("Cannot delete a missing extension") })

	def applyInternal(): Extension = transactional() {
		extension._state = ExtensionState.Revoked
		assignment.extensions.remove(extension)
		extension.attachments.asScala.foreach(delete(_))
		delete(extension)
		extension
	}
}

trait DeleteExtensionCommandNotification extends Notifies[Extension, Option[Extension]] {
	self: ModifyExtensionCommandState =>

	def emit(extension: Extension): Seq[ExtensionRevokedNotification] = {
		val notification = Notification.init(new ExtensionRevokedNotification, submitter.apparentUser, Seq(extension.assignment))
		notification.recipientUniversityId = extension.universityId
		Seq(notification)
	}
}


trait DeleteExtensionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyExtensionCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Extension.Delete, assignment)
	}
}
