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

object ViewModuleAttendanceCommand {
	def apply(module: Module) =
		new ViewModuleAttendanceCommand(module)
			with ComposableCommand[SortedMap[SmallGroupSet, SortedMap[SmallGroup, SmallGroupAttendanceInformation]]]
			with ViewModuleAttendancePermissions
			with ReadOnly with Unaudited {
		override lazy val eventName = "ViewModuleAttendance"
	}
}

class ViewModuleAttendanceCommand(val module: Module)
	extends CommandInternal[SortedMap[SmallGroupSet, SortedMap[SmallGroup, SmallGroupAttendanceInformation]]] with ViewModuleAttendanceState {
	
	override def applyInternal() = {
		SortedMap(module.groupSets.asScala.map { set =>
			(set -> ViewSmallGroupSetAttendanceCommand(set).apply())
		}.toSeq:_*)
	}
	
}

trait ViewModuleAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewModuleAttendanceState =>
	
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, module)
	}
}

trait ViewModuleAttendanceState {
	def module: Module
}