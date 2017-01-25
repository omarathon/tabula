package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.notifications.coursework.{ExtensionRequestCreatedNotification, ExtensionRequestModifiedNotification}
import uk.ac.warwick.tabula.data.model.{Notification, StudentMember, ToEntityReference}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringExtensionServiceComponent, ExtensionServiceComponent, NotificationServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.AnonymousUser

object HandleDeceasedStudentCommand {
	def apply(student: StudentMember) =
		new HandleDeceasedStudentCommandInternal(student)
			with AutowiringExtensionServiceComponent
			with NotificationServiceComponent
			with ComposableCommand[Unit]
			with HandleDeceasedStudentPermissions
			with Unaudited
}


class HandleDeceasedStudentCommandInternal(student: StudentMember) extends CommandInternal[Unit] {

	self: ExtensionServiceComponent with NotificationServiceComponent =>

	override def applyInternal(): Unit = {
		if (student.deceased) {
			val extensions = handlePendingExtensions()

			cleanUpNotifications(extensions)
		}
	}

	private def handlePendingExtensions(): Seq[Extension] = {
		val extensions = extensionService.getPreviousExtensions(MemberOrUser(student).asUser).filter(_.awaitingReview)
		extensions.foreach(_.reject("Automatically removing request from deceased student"))
		extensions.foreach(extensionService.saveOrUpdate)
		extensions
	}

	private def cleanUpNotifications(entities: Seq[ToEntityReference]): Unit = {
		val user = new AnonymousUser()
		val notifications = entities.flatMap {
			case extension: Extension =>
				val extensionNotifications =
					notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestCreatedNotification](extension) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestModifiedNotification](extension)
				extensionNotifications.foreach(_.actionCompleted(user))
				extensionNotifications
		}

		if (notifications.nonEmpty) {
			notificationService.update(
				notifications.map(_.asInstanceOf[Notification[_, _]]),
				user
			)
		}
	}

}

trait HandleDeceasedStudentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

}
