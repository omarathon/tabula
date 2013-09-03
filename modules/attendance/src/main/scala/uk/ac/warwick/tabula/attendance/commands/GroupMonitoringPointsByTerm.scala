package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.{DateTimeConstants, DateMidnight}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import scala.collection.JavaConverters._


trait GroupMonitoringPointsByTerm extends TermServiceComponent {
	def groupByTerm(monitoringPoints: Seq[MonitoringPoint], academicYear: AcademicYear) = {
		lazy val weeksForYear =
			termService.getAcademicWeeksForYear(new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1))
				.asScala.map { pair => pair.getLeft -> pair.getRight } // Utils pairs to Scala pairs
				.toMap
		val day = DayOfWeek.Monday
		monitoringPoints.groupBy {
			case point => termService.getTermFromDateIncludingVacations(
				weeksForYear(point.week).getStart.withDayOfWeek(day.jodaDayOfWeek)
			).getTermTypeAsString
		}
	}
}
