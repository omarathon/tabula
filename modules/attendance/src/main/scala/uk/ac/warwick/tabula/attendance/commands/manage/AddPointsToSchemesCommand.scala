package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyLists

object AddPointsToSchemesCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new AddPointsToSchemesCommandInternal(department, academicYear)
			with ComposableCommand[Map[AttendanceMonitoringScheme, Boolean]]
			with AutowiringAttendanceMonitoringServiceComponent
			with AddPointsToSchemesPermissions
			with AddPointsToSchemesCommandState
			with ReadOnly with Unaudited
}


class AddPointsToSchemesCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Map[AttendanceMonitoringScheme, Boolean]] with AddPointsToSchemesCommandState {

	self: AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		attendanceMonitoringService.listSchemes(department, academicYear).map{
			scheme => scheme -> schemes.contains(scheme)
		}.toMap
	}

}

trait AddPointsToSchemesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AddPointsToSchemesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait AddPointsToSchemesCommandState {
	def department: Department
	def academicYear: AcademicYear

	var schemes: JList[AttendanceMonitoringScheme] = LazyLists.create()
}
