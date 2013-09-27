package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.{DateTimeConstants, DateMidnight}
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek


trait GroupMonitoringPointsByTerm extends TermServiceComponent {
	def groupByTerm(monitoringPoints: Seq[MonitoringPoint], academicYear: AcademicYear): Map[String, Seq[MonitoringPoint]] = {
		val approxStartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1)
		val day = DayOfWeek.Thursday
		lazy val weeksForYear = termService.getAcademicWeeksForYear(approxStartDate).toMap

		monitoringPoints groupBy { point =>
			val date = weeksForYear(point.validFromWeek).getStart.withDayOfWeek(day.jodaDayOfWeek)
			termService.getTermFromDateIncludingVacations(date).getTermTypeAsString
		}
	}

}
