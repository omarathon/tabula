package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance.{GroupedPoint, GroupsPoints}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringPointType, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.util.web.UriBuilder

import scala.collection.JavaConverters._

case class FindPointsResult(
	termGroupedPoints: Map[String, Seq[GroupedPoint]],
	monthGroupedPoints: Map[String, Seq[GroupedPoint]],
	courseworkAssignmentPoints:Seq[AttendanceMonitoringPoint]
)

trait CourseworkPoints {
	def assignmentPoints(points: Seq[AttendanceMonitoringPoint]): Seq[AttendanceMonitoringPoint] = {
		points.filter { point => point.isSpecificAssignmentPoint }
	}
}


object FindPointsCommand {
	def apply(department: Department, academicYear: AcademicYear, restrictedStyle: Option[AttendanceMonitoringPointStyle]) =
		new FindPointsCommandInternal(department, academicYear, restrictedStyle)
			with ComposableCommand[FindPointsResult]
			with AutowiringAttendanceMonitoringServiceComponent
			with GroupsPoints
			with CourseworkPoints
			with FindPointsPermissions
			with FindPointsCommandState
			with ReadOnly with Unaudited
}


class FindPointsCommandInternal(val department: Department, val academicYear: AcademicYear, val restrictedStyle: Option[AttendanceMonitoringPointStyle])
	extends CommandInternal[FindPointsResult] {

	self: AttendanceMonitoringServiceComponent with FindPointsCommandState with GroupsPoints with CourseworkPoints =>

	override def applyInternal(): FindPointsResult = {
		val points = attendanceMonitoringService.findPoints(department, academicYear, findSchemes.asScala, types.asScala, styles.asScala)

		restrictedStyle match {
			case Some(AttendanceMonitoringPointStyle.Date) =>
				FindPointsResult(Map(), groupByMonth(points), assignmentPoints(points))
			case Some(AttendanceMonitoringPointStyle.Week) =>
				FindPointsResult(groupByTerm(points), Map(), assignmentPoints(points))
			case _ =>
				FindPointsResult(groupByTerm(points), groupByMonth(points), assignmentPoints(points))
		}
	}

}

trait FindPointsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: FindPointsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, department)
	}

}

trait FindPointsCommandState {
	def department: Department
	def academicYear: AcademicYear
	def restrictedStyle: Option[AttendanceMonitoringPointStyle]

	// Bind variables
	var findSchemes: JList[AttendanceMonitoringScheme] = LazyLists.create[AttendanceMonitoringScheme]()
	var types: JList[AttendanceMonitoringPointType] = LazyLists.createWithFactory[AttendanceMonitoringPointType] {
		() => null
	}
	var styles: JList[AttendanceMonitoringPointStyle] = LazyLists.createWithFactory[AttendanceMonitoringPointStyle] {
		() => null
	}

	def serializeFilter: String = {
		val result = new UriBuilder()
		findSchemes.asScala.foreach(scheme => result.addQueryParameter("findSchemes", scheme.id))
		types.asScala.foreach(t => result.addQueryParameter("types", t.dbValue))
		styles.asScala.foreach(style => result.addQueryParameter("styles", style.dbValue))
		if (result.getQuery == null)
			""
		else
			result.getQuery
	}
}
