package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.attendance.commands.{AutowiringSecurityServicePermissionsAwareRoutes, PermissionsAwareRoutes}
import uk.ac.warwick.tabula.permissions.{Permissions, CheckablePermission}
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}

object ViewSchemesCommand {
	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new ViewSchemesCommandInternal(department, academicYear, user)
		with AutowiringSecurityServicePermissionsAwareRoutes
		with AutowiringAttendanceMonitoringServiceComponent
		with ComposableCommand[Seq[AttendanceMonitoringScheme]]
		with ViewSchemesPermissions
		with ViewSchemesCommandState
		with Unaudited with ReadOnly
}


class ViewSchemesCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
	extends CommandInternal[Seq[AttendanceMonitoringScheme]] {

	self: AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		attendanceMonitoringService.listSchemes(department, academicYear)
	}

}

trait ViewSchemesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods with PermissionsAwareRoutes {

	self: ViewSchemesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.MonitoringPoints.Manage, mandatory(department))) ++
				routesForPermission(user, Permissions.MonitoringPoints.Manage, department).map { route => CheckablePermission(Permissions.MonitoringPoints.Manage, route) }
		)
	}

}

trait ViewSchemesCommandState {
	def department: Department
	def academicYear: AcademicYear
	def user: CurrentUser
}
