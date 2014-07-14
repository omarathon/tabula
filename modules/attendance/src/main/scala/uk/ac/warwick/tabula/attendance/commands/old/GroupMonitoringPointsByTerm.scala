package uk.ac.warwick.tabula.attendance.commands.old

import org.joda.time.{DateMidnight, DateTimeConstants}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.attendance.{CommonMonitoringPointProperties, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.services.TermServiceComponent

case class GroupedMonitoringPoint(
	name: String,
	validFromWeek: Int,
	requiredFromWeek: Int,
	// Flag shows whether route is in a given department's routes
	routes: Seq[(Route, Boolean)],
	pointId: String
)

trait GroupMonitoringPointsByTerm extends TermServiceComponent {
	def groupByTerm[A <: CommonMonitoringPointProperties](monitoringPoints: Seq[A], academicYear: AcademicYear): Map[String, Seq[A]] = {
		val approxStartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1)
		val day = DayOfWeek.Thursday
		lazy val weeksForYear = termService.getAcademicWeeksForYear(approxStartDate).toMap

		monitoringPoints groupBy { point =>
			val date = weeksForYear(point.validFromWeek).getStart.withDayOfWeek(day.jodaDayOfWeek)
			termService.getTermFromDateIncludingVacations(date).getTermTypeAsString
		} map { case (term, points) => term -> points.sortBy(p => (p.validFromWeek, p.requiredFromWeek)) }
	}

	def groupSimilarPointsByTerm[A <: CommonMonitoringPointProperties](
		monitoringPoints: Seq[A],
		deptRoutes: Seq[Route],
		academicYear: AcademicYear
	): Map[String, Seq[GroupedMonitoringPoint]] = {
		groupByTerm(monitoringPoints, academicYear).map{
			case (term, points) => term -> points.groupBy{
				mp => GroupedMonitoringPoint(mp.name.toLowerCase, mp.validFromWeek, mp.requiredFromWeek, Seq(), "")
			}.map{
				case (point, groupedPoints) => {
					GroupedMonitoringPoint(
						groupedPoints.head.name,
						point.validFromWeek,
						point.requiredFromWeek,
						groupedPoints.flatMap {
							case mp: MonitoringPoint => Some(mp.pointSet.route)
							case _ => None
						}.distinct.sorted(Route.DegreeTypeOrdering).map{
							r => (r, deptRoutes.contains(r))
						},
						groupedPoints.head.id
					)
				}
			}.toSeq.sortBy(p => (p.validFromWeek, p.requiredFromWeek))
		}
	}

}
