package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.model.notifications.attendance.UnlinkedAttendanceMonitoringSchemeNotification
import uk.ac.warwick.tabula.data.model.{Department, Notification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object UnlinkAttendanceMonitoringSchemeCommand {
	def apply() =
		new UnlinkAttendanceMonitoringSchemeCommandInternal
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[Map[Department, Seq[AttendanceMonitoringScheme]]]
			with UnlinkAttendanceMonitoringSchemeDescription
			with UnlinkAttendanceMonitoringSchemePermissions
			with UnlinkAttendanceMonitoringSchemeNotifications
}


class UnlinkAttendanceMonitoringSchemeCommandInternal extends CommandInternal[Map[Department, Seq[AttendanceMonitoringScheme]]] {

	self: AttendanceMonitoringServiceComponent =>

	override def applyInternal(): Map[Department, Seq[AttendanceMonitoringScheme]] = {
		val academicYear = AcademicYear.now()
		val schemeMap = transactional() {
			attendanceMonitoringService.findSchemesLinkedToSITSByDepartment(academicYear)
		}
		schemeMap.map{ case(department, schemes) => department -> schemes.map{scheme => transactional() {
			scheme.memberQuery = ""
			scheme.members.includedUserIds = ((scheme.members.staticUserIds diff scheme.members.excludedUserIds) ++ scheme.members.includedUserIds).distinct
			attendanceMonitoringService.saveOrUpdate(scheme)
			scheme
		}}}
	}

}

trait UnlinkAttendanceMonitoringSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.UpdateMembership)
	}

}

trait UnlinkAttendanceMonitoringSchemeDescription extends Describable[Map[Department, Seq[AttendanceMonitoringScheme]]] {

	override lazy val eventName = "UnlinkAttendanceMonitoringScheme"

	override def describe(d: Description) {

	}

	override def describeResult(d: Description, result: Map[Department, Seq[AttendanceMonitoringScheme]]) {
		d.property("updatedSchemes" -> result.map{case(dept, schemes) => dept.code -> schemes.map(_.id)})
	}
}

trait UnlinkAttendanceMonitoringSchemeNotifications extends Notifies[Map[Department, Seq[AttendanceMonitoringScheme]], Map[Department, Seq[AttendanceMonitoringScheme]]] {

	def emit(result: Map[Department, Seq[AttendanceMonitoringScheme]]): Seq[UnlinkedAttendanceMonitoringSchemeNotification] = {
		result.map { case (department, schemes) =>
			Notification.init(new UnlinkedAttendanceMonitoringSchemeNotification, null, schemes, department)
		}.toSeq
	}
}
