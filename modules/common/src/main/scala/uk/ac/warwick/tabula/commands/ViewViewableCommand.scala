package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget

class ViewViewableCommand[V <: PermissionsTarget : Manifest](val permission: Permission, val value: V) extends Command[V] with ReadOnly with Unaudited {
	PermissionCheck(permission, value)

	override def applyInternal() = value
}