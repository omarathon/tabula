package uk.ac.warwick.tabula.groups.commands.admin

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.ViewSmallGroupAttendanceCommand
import uk.ac.warwick.tabula.groups.commands.ViewSmallGroupAttendanceCommand._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.SecurityServiceComponent
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.AutowiringSecurityServiceComponent
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.data.model.Department

object ViewDepartmentAttendanceCommand {
	def apply(department: Department, user: CurrentUser) =
		new AdminDepartmentHomeCommand(department, user)
			with ViewRegisterPermissionDefinition
			with AdminDepartmentHomePermissions
			with AutowiringSecurityServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with ComposableCommand[Seq[Module]]
			with ReadOnly with Unaudited {
		override lazy val eventName = "ViewDepartmentAttendance"
	}
}

trait ViewRegisterPermissionDefinition extends AdminDepartmentHomePermissionDefinition {
	val requiredPermission = Permissions.SmallGroupEvents.Register
}