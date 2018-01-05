package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

object ViewSmallGroupSetsWithMissingMapLocationsCommand {
	val RequiredPermission = Permissions.SmallGroups.Update

	def apply(academicYear: AcademicYear, department: Department) =
		new ViewSmallGroupSetsWithMissingMapLocationsCommand(academicYear, department)
			with ComposableCommand[Seq[(SmallGroupSet, Seq[SmallGroupEvent])]]
			with ViewSmallGroupSetsWithMissingMapLocationPermissions
			with AutowiringSmallGroupServiceComponent
			with ReadOnly with Unaudited {
			override lazy val eventName = "ViewSmallGroupSetsWithMissingMapLocations"
		}
}

class ViewSmallGroupSetsWithMissingMapLocationsCommand(val academicYear: AcademicYear, val department: Department)
	extends CommandInternal[Seq[(SmallGroupSet, Seq[SmallGroupEvent])]]
		with ViewSmallGroupSetsWithMissingMapLocationState {
	self: SmallGroupServiceComponent =>

	override def applyInternal(): Seq[(SmallGroupSet, Seq[SmallGroupEvent])] = {
		smallGroupService.listSmallGroupSetsWithEventsWithoutMapLocation(academicYear, Some(department))
			.toSeq.sortBy { case (set, _) => (set.module, set) }
	}
}

trait ViewSmallGroupSetsWithMissingMapLocationPermissions extends RequiresPermissionsChecking {
	self: ViewSmallGroupSetsWithMissingMapLocationState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(ViewSmallGroupSetsWithMissingMapLocationsCommand.RequiredPermission, department)
	}
}

trait ViewSmallGroupSetsWithMissingMapLocationState {
	def academicYear: AcademicYear

	def department: Department
}