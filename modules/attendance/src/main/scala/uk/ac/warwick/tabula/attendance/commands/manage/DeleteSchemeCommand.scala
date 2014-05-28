package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{SelfValidating, Description, Describable, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.JavaConverters._
import org.springframework.validation.Errors

object DeleteSchemeCommand {
	def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
		new DeleteSchemeCommandInternal(scheme, user)
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with DeleteSchemeCommandState
			with DeleteSchemeDescription
			with DeleteSchemePermissions
			with DeleteSchemeValidation

}

class DeleteSchemeCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
	extends CommandInternal[AttendanceMonitoringScheme] {

	self: AttendanceMonitoringServiceComponent =>

		override def applyInternal() = {
			attendanceMonitoringService.deleteScheme(scheme)
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

	self: AttendanceMonitoringServiceComponent =>

	def scheme: AttendanceMonitoringScheme
	def user: CurrentUser

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

		val pointsWithCheckpoints = scheme.points.asScala.filter {
			point => attendanceMonitoringService.countCheckpointsForPoint(point) > 0
		}

		if (!pointsWithCheckpoints.isEmpty) {
			errors.reject("attendanceMonitoringScheme.hasCheckpoints.remove")
		}

	}

}