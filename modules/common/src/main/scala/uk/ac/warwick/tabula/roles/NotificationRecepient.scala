package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification}
import uk.ac.warwick.tabula.permissions._
import language.existentials

case class NotificationRecepient(notification: Notification[_>: Null <: ToEntityReference, _])
	extends BuiltInRole(NotificationRecepientRoleDefinition, notification)

object NotificationRecepientRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Notification recepient"

	GrantsScopedPermission(
		Permissions.Notification.Dismiss
	)

}