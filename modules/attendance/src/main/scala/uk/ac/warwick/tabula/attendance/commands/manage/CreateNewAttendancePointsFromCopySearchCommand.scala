package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}

case class CreateNewAttendancePointsFromCopySearchCommandResult(
	schemes: Seq[AttendanceMonitoringScheme],
	sets: Seq[MonitoringPointSet]
)

object CreateNewAttendancePointsFromCopySearchCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new CreateNewAttendancePointsFromCopySearchCommandInternal(department, academicYear)
			with ComposableCommand[CreateNewAttendancePointsFromCopySearchCommandResult]
			with AutowiringAttendanceMonitoringServiceComponent
			with CreateNewAttendancePointsFromCopySearchPermissions
			with CreateNewAttendancePointsFromCopySearchCommandState
			with ReadOnly with Unaudited
}


class CreateNewAttendancePointsFromCopySearchCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[CreateNewAttendancePointsFromCopySearchCommandResult] {

	self: AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		CreateNewAttendancePointsFromCopySearchCommandResult(
			attendanceMonitoringService.listSchemes(department, academicYear).sortBy(_.displayName),
			attendanceMonitoringService.listOldSets(department, academicYear).sortBy(_.route.code)
		)
	}

}

trait CreateNewAttendancePointsFromCopySearchPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CreateNewAttendancePointsFromCopySearchCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait CreateNewAttendancePointsFromCopySearchCommandState {
	def department: Department
	def academicYear: AcademicYear
}
