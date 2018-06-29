package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.{UAMAuditFirstNotification, UAMAuditNotification, UAMAuditSecondNotification}
import uk.ac.warwick.tabula.data.model.{Department, Notification}
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.UserAccessMgrRoleDefinition
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object UserAccessManagerAuditCommand {

	def apply(choice: String): Appliable[Seq[UserAccessManagerWithDepartments]] = {
		val notification: Option[UAMAuditFirstNotification] = choice match {
			case "second" => Some(new UAMAuditSecondNotification)
			case "first" => Some(new UAMAuditFirstNotification)
			case _ => None
		}
		if (notification.isDefined) apply(notification.get) else apply
	}

	def apply: Appliable[Seq[UserAccessManagerWithDepartments]] = {
		class EmptyUserAccessManagerAuditCommandInternal extends CommandInternal[Seq[UserAccessManagerWithDepartments]] {
			self: PermissionsServiceComponent =>
			override def applyInternal(): Seq[UserAccessManagerWithDepartments] = Seq.empty[UserAccessManagerWithDepartments]
		}
		new EmptyUserAccessManagerAuditCommandInternal
			with ComposableCommand[Seq[UserAccessManagerWithDepartments]]
			with AutowiringPermissionsServiceComponent
			with UserAccessManagerAuditCommandPermissions
			with UserAccessManagerAuditCommandDescription
			with ReadOnly
	}

	def apply[A <: UAMAuditNotification](notification: A): Appliable[Seq[UserAccessManagerWithDepartments]] =
		new UserAccessManagerAuditCommandInternal[A](notification)
			with ComposableCommand[Seq[UserAccessManagerWithDepartments]]
			with AutowiringPermissionsServiceComponent
			with UserAccessManagerAuditCommandNotifications[A]
			with UserAccessManagerAuditCommandPermissions
			with UserAccessManagerAuditCommandDescription
			with ReadOnly
}

case class UserAccessManagerWithDepartments(user: User, departments: Seq[Department])

class UserAccessManagerAuditCommandInternal[A <: UAMAuditNotification](val notification: A) extends CommandInternal[Seq[UserAccessManagerWithDepartments]] {
	self: PermissionsServiceComponent =>

	override def applyInternal(): Seq[UserAccessManagerWithDepartments] =
		permissionsService.getAllGrantedRolesForDefinition(UserAccessMgrRoleDefinition).flatMap(_.users.users).distinct.map { user =>
			val roles = permissionsService.getAllGrantedRolesFor(new CurrentUser(user, user))
			UserAccessManagerWithDepartments(
				user = user,
				departments = roles.filter(_.roleDefinition == UserAccessMgrRoleDefinition).flatMap(_.scopeDepartment)
			)
		}
}

trait UserAccessManagerAuditCommandNotifications[A <: UAMAuditNotification] extends Notifies[Seq[UserAccessManagerWithDepartments], User] {
	val notification: A

	override def emit(result: Seq[UserAccessManagerWithDepartments]): Seq[UAMAuditNotification] = {
		result.map { uam =>
			def makeNotification(n: UAMAuditNotification): UAMAuditNotification = Notification.init(n, uam.user, uam.departments)

			notification match {
				case _: UAMAuditSecondNotification => makeNotification(new UAMAuditSecondNotification)
				case _: UAMAuditFirstNotification => makeNotification(new UAMAuditFirstNotification)
				case _ => throw new IllegalStateException("Invalid UAM audit notification")
			}
		}
	}
}

trait UserAccessManagerAuditCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking): Unit = p.PermissionCheck(Permissions.RolesAndPermissions.Read, PermissionsTarget.Global)
}

trait UserAccessManagerAuditCommandDescription extends Describable[Seq[UserAccessManagerWithDepartments]] {
	override def describe(d: Description): Unit = {}

	override def describeResult(d: Description, result: Seq[UserAccessManagerWithDepartments]): Unit = d.users(result.map(_.user))
}