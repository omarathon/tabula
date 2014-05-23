package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringScheme, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services.TermServiceComponent
import java.text.DateFormatSymbols
import org.joda.time.{DateTimeConstants, DateMidnight}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.AcademicYear

case class GroupedPoint(
	templatePoint: AttendanceMonitoringPoint,
	schemes: Seq[AttendanceMonitoringScheme]
)

case class GroupedOldPoint(
	templatePoint: MonitoringPoint,
	sets: Seq[MonitoringPointSet]
)

trait GroupsPoints {

	self: TermServiceComponent =>

	def groupByTerm(points: Seq[AttendanceMonitoringPoint]): Map[String, Seq[GroupedPoint]] = {
		points
			// only week points group by term
			.filter(_.scheme.pointStyle == AttendanceMonitoringPointStyle.Week)
			// group by term (using pre-calculated date)
			.groupBy{ point =>
				termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString
			}
			// group each term's similar points (by name and weeks)
			.map{ case (term, nonGroupedPoints) => term ->
				nonGroupedPoints.groupBy{ point => (point.name.toLowerCase, point.startWeek, point.endWeek)}
			}
			// transform similar points into 1 grouped point
			.map{ case (term, groupedPoints) => term ->
				groupedPoints.map{ case(_, pointGroup) => GroupedPoint(pointGroup.head, pointGroup.map(_.scheme))}
					.toSeq.sortBy(p => (p.templatePoint.startWeek, p.templatePoint.endWeek))
			}
	}

	def groupByMonth(points: Seq[AttendanceMonitoringPoint]): Map[String, Seq[GroupedPoint]] = {
		points
			// only date points group by month
			.filter(_.scheme.pointStyle == AttendanceMonitoringPointStyle.Date)
			// group by month
			.groupBy{ point => (point.startDate.monthOfYear, point.startDate.year) }
			// group each month's similar points (by name and weeks)
			.map{ case (monthYearPair, nonGroupedPoints) => monthYearPair ->
				nonGroupedPoints.groupBy{ point => (point.name.toLowerCase, point.startWeek, point.endWeek)}
			}
			// transform similar points into 1 grouped point
			.map{ case (monthYearPair, groupedPoints) =>
				new DateFormatSymbols().getMonths.array(monthYearPair._1.get - 1) + " " + monthYearPair._2.get ->
					groupedPoints.map{ case(_, pointGroup) => GroupedPoint(pointGroup.head, pointGroup.map(_.scheme))}
						.toSeq.sortBy(p => (p.templatePoint.startWeek, p.templatePoint.endWeek))
			}
	}

	def groupOldByTerm(points: Seq[MonitoringPoint], academicYear: AcademicYear): Map[String, Seq[GroupedOldPoint]] = {
		val approxStartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1)
		val day = DayOfWeek.Thursday
		lazy val weeksForYear = termService.getAcademicWeeksForYear(approxStartDate).toMap

		points
			// group by term (have to calculate date)
			.groupBy{ point =>
				val date = weeksForYear(point.validFromWeek).getStart.withDayOfWeek(day.jodaDayOfWeek)
				termService.getTermFromDateIncludingVacations(date).getTermTypeAsString
			}
			// group each term's similar points (by name and weeks)
			.map{ case (term, nonGroupedPoints) => term ->
			nonGroupedPoints.groupBy{ point => (point.name.toLowerCase, point.validFromWeek, point.requiredFromWeek)}
		}
			// transform similar points into 1 grouped point
			.map{ case (term, groupedPoints) => term ->
			groupedPoints.map{ case(_, pointGroup) => GroupedOldPoint(pointGroup.head, pointGroup.map(_.pointSet))}
				.toSeq.sortBy(p => (p.templatePoint.validFromWeek, p.templatePoint.requiredFromWeek))
		}
	}
}
