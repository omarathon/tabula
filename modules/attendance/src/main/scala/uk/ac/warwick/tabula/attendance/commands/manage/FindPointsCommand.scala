package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointType, MonitoringPointSet, AttendanceMonitoringPointStyle, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AutowiringTermServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyLists
import collection.JavaConverters._
import uk.ac.warwick.util.web.UriBuilder
import uk.ac.warwick.tabula.attendance.commands.{GroupedOldPoint, GroupedPoint, GroupsPoints}

case class FindPointsResult(
	termGroupedPoints: Map[String, Seq[GroupedPoint]],
	monthGroupedPoints: Map[String, Seq[GroupedPoint]],
	termGroupedOldPoints: Map[String, Seq[GroupedOldPoint]]
)

object FindPointsCommand {
	def apply(department: Department, academicYear: AcademicYear, restrictedStyle: Option[AttendanceMonitoringPointStyle]) =
		new FindPointsCommandInternal(department, academicYear, restrictedStyle)
			with ComposableCommand[FindPointsResult]
			with AutowiringTermServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with GroupsPoints
			with FindPointsPermissions
			with FindPointsCommandState
			with ReadOnly with Unaudited
}


class FindPointsCommandInternal(val department: Department, val academicYear: AcademicYear, val restrictedStyle: Option[AttendanceMonitoringPointStyle])
	extends CommandInternal[FindPointsResult] {

	self: AttendanceMonitoringServiceComponent with FindPointsCommandState with GroupsPoints =>

	override def applyInternal() = {
		restrictedStyle match {
			case Some(AttendanceMonitoringPointStyle.Date) =>
				val points = attendanceMonitoringService.findPoints(department, academicYear, findSchemes.asScala, types.asScala, styles.asScala)
				FindPointsResult(Map(), groupByMonth(points), Map())
			case Some(AttendanceMonitoringPointStyle.Week) =>
				if (academicYear.startYear < 2014) {
					val points = attendanceMonitoringService.findOldPoints(department, academicYear, sets.asScala, types.asScala)
					FindPointsResult(Map(), Map(), groupOldByTerm(points, academicYear))
				} else {
					val points = attendanceMonitoringService.findPoints(department, academicYear, findSchemes.asScala, types.asScala, styles.asScala)
					FindPointsResult(groupByTerm(points), Map(), Map())
				}
			case _ =>
				val points = attendanceMonitoringService.findPoints(department, academicYear, findSchemes.asScala, types.asScala, styles.asScala)
				FindPointsResult(groupByTerm(points), groupByMonth(points), Map())
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
	var findSchemes: JList[AttendanceMonitoringScheme] = LazyLists.create()
	var sets: JList[MonitoringPointSet] = LazyLists.create()
	var types: JList[AttendanceMonitoringPointType] = LazyLists.create {
		() => null
	}
	var styles: JList[AttendanceMonitoringPointStyle] = LazyLists.create {
		() => null
	}

	def serializeFilter = {
		val result = new UriBuilder()
		findSchemes.asScala.foreach(scheme => result.addQueryParameter("findSchemes", scheme.id))
		sets.asScala.foreach(set => result.addQueryParameter("sets", set.id))
		types.asScala.foreach(t => result.addQueryParameter("types", t.dbValue))
		styles.asScala.foreach(style => result.addQueryParameter("styles", style.dbValue))
		if (result.getQuery == null)
			""
		else
			result.getQuery
	}
}
