package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringScheme, AttendanceMonitoringPointStyle, AttendanceMonitoringPoint, AttendanceMonitoringTemplate}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.commands.{CommandInternal, Unaudited, ReadOnly, ComposableCommand}
import uk.ac.warwick.tabula.services.{TermServiceComponent, AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AutowiringTermServiceComponent}
import uk.ac.warwick.tabula.attendance.commands.GroupsPoints
import collection.JavaConverters._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.data.model.Department

object AttendancePointsFromTemplateSchemeCommand {
	def apply(templateScheme: AttendanceMonitoringTemplate, academicYear: AcademicYear, department: Department) = {
		new AttendancePointsFromTemplateSchemeCommandInternal(templateScheme, academicYear, department)
			with ComposableCommand[Seq[AttendanceMonitoringPoint]]
			with PubliclyVisiblePermissions
			with AttendancePointsFromTemplateSchemeCommandState
			with AutowiringTermServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with GroupsPoints
			with ReadOnly with Unaudited
	}
}


class AttendancePointsFromTemplateSchemeCommandInternal(val templateScheme: AttendanceMonitoringTemplate, val academicYear: AcademicYear, val department: Department)
	extends CommandInternal[Seq[AttendanceMonitoringPoint]] {
	self: AttendanceMonitoringServiceComponent with GroupsPoints with TermServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringPoint] = {
		val attendanceMonitoringPoints = getPoints
		attendanceMonitoringPoints
	}

	def determineYear(date: LocalDate): Int = {
		if (date.getMonthOfYear < 10) academicYear.endYear else academicYear.startYear
	}

	def getPoints: Seq[AttendanceMonitoringPoint] = {
		val weeksForYear = termService.getAcademicWeeksForYear(academicYear.dateInTermOne).toMap
		val stubScheme = new AttendanceMonitoringScheme
		stubScheme.pointStyle = templateScheme.pointStyle
		stubScheme.academicYear = academicYear

		val attendanceMonitoringPoints =
			templateScheme.points.asScala.map { templatePoint =>
				val point = templatePoint.toPoint
				templateScheme.pointStyle match {
					case AttendanceMonitoringPointStyle.Date =>
						point.startDate = templatePoint.startDate.withYear(determineYear(templatePoint.startDate))
						point.endDate = templatePoint.endDate.withYear(determineYear(templatePoint.endDate))
					case AttendanceMonitoringPointStyle.Week =>
						point.startWeek = templatePoint.startWeek
						point.endWeek = templatePoint.endWeek
						point.startDate = weeksForYear(templatePoint.startWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate
						point.endDate = weeksForYear(templatePoint.endWeek).getStart.withDayOfWeek(DayOfWeek.Monday.jodaDayOfWeek).toLocalDate.plusDays(6)
				}
				point.scheme = stubScheme
				point
			}
		attendanceMonitoringPoints
	}

	def getGroupedPoints: FindPointsResult = {
		val points = getPoints
		templateScheme.pointStyle match {
			case AttendanceMonitoringPointStyle.Week => FindPointsResult(groupByTerm(points), Map(), Map())
			case AttendanceMonitoringPointStyle.Date => FindPointsResult(Map(), groupByMonth(points), Map())
			case _ => FindPointsResult(groupByTerm(points), groupByMonth(points), Map())
		}
	}
}

trait AttendancePointsFromTemplateSchemeCommandState {
	def department: Department
	def academicYear: AcademicYear
	def templateScheme: AttendanceMonitoringTemplate
}
