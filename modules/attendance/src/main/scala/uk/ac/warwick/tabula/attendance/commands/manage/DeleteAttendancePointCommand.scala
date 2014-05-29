package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringScheme, AttendanceMonitoringPointStyle, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}

object DeleteAttendancePointCommand {
	def apply(department: Department, templatePoint: AttendanceMonitoringPoint, findPointsResult: FindPointsResult) =
		new DeleteAttendancePointCommandInternal(department, templatePoint, findPointsResult)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with AutowiringAttendanceMonitoringServiceComponent
			with DeleteAttendancePointValidation
			with DeleteAttendancePointDescription
			with DeleteAttendancePointPermissions
			with DeleteAttendancePointCommandState
}


class DeleteAttendancePointCommandInternal(
	val department: Department,
	val templatePoint: AttendanceMonitoringPoint,
	val findPointsResult: FindPointsResult
) extends CommandInternal[Seq[AttendanceMonitoringPoint]] {

	self: DeleteAttendancePointCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		pointsToDelete.foreach(attendanceMonitoringService.deletePoint)
		pointsToDelete
	}

}


trait DeleteAttendancePointValidation extends SelfValidating {

	self: DeleteAttendancePointCommandState with AttendanceMonitoringServiceComponent =>

	override def validate(errors: Errors) {

		val pointsWithCheckpoints = pointsToDelete.filter {
			point => attendanceMonitoringService.countCheckpointsForPoint(point) > 0
		}

		if (!pointsWithCheckpoints.isEmpty) {
			errors.reject("attendanceMonitoringPoints.hasCheckpoints.remove")
		}
	}

}

trait DeleteAttendancePointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DeleteAttendancePointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait DeleteAttendancePointDescription extends Describable[Seq[AttendanceMonitoringPoint]] {

	self: DeleteAttendancePointCommandState =>

	override lazy val eventName = "DeleteAttendancePoint"

	override def describe(d: Description) {
		d.attendanceMonitoringPoints(pointsToDelete)
	}
}

trait DeleteAttendancePointCommandState {

	def department: Department
	def templatePoint: AttendanceMonitoringPoint
	def findPointsResult: FindPointsResult
	lazy val pointStyle: AttendanceMonitoringPointStyle = templatePoint.scheme.pointStyle

	def pointsToDelete: Seq[AttendanceMonitoringPoint] = (pointStyle match {
		case AttendanceMonitoringPointStyle.Week => findPointsResult.termGroupedPoints
		case AttendanceMonitoringPointStyle.Date => findPointsResult.monthGroupedPoints
	}).flatMap(_._2)
		.find(p => p.templatePoint.id == templatePoint.id)
		.getOrElse(throw new IllegalArgumentException)
		.points

	def schemesToEdit: Seq[AttendanceMonitoringScheme] = pointsToDelete.map(_.scheme)
}
