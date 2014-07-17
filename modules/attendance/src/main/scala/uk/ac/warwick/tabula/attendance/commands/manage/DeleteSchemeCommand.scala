package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.commands.{SelfValidating, Description, Describable, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.JavaConverters._
import org.springframework.validation.Errors

object DeleteSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme) =
		new DeleteSchemeCommandInternal(scheme)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with DeleteSchemeCommandState
			with DeleteSchemeDescription
			with DeleteSchemePermissions
			with DeleteSchemeValidation

}

class DeleteSchemeCommandInternal(val scheme: AttendanceMonitoringScheme)
	extends CommandInternal[AttendanceMonitoringScheme] {

	self: AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

	override def applyInternal() = {
		attendanceMonitoringService.deleteScheme(scheme)

		val students = profileService.getAllMembersWithUniversityIds(scheme.members.members).flatMap {
			case student: StudentMember => Option(student)
			case _ => None
		}
		attendanceMonitoringService.updateCheckpointTotalsAsync(students, scheme.department, scheme.academicYear)

		scheme
	}
}

trait DeleteSchemeDescription extends Describable[AttendanceMonitoringScheme] {

	self: DeleteSchemeCommandState =>

	override lazy val eventName = "DeleteScheme"

	override def describe(d: Description) {
		d.attendanceMonitoringScheme(scheme)
	}
}

trait DeleteSchemeCommandState  {
	def scheme: AttendanceMonitoringScheme
}


trait DeleteSchemePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DeleteSchemeCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait DeleteSchemeValidation extends SelfValidating {

	self: DeleteSchemeCommandState with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {
		if (scheme.points.asScala.exists { point => attendanceMonitoringService.countCheckpointsForPoint(point) > 0 }) {
			errors.reject("attendanceMonitoringScheme.hasCheckpoints.remove")
		}
	}

}