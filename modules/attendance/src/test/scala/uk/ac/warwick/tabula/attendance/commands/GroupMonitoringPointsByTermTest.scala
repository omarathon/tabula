package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.data.model.Route
import org.joda.time.{Interval, DateTimeConstants, DateMidnight}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.util.termdates.Term
import uk.ac.warwick.tabula.services.Vacation
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek

class GroupMonitoringPointsByTermTest extends TestBase with Mockito {

	trait TestSupport extends TermServiceComponent {
		val termService = mock[TermService]
	}

	trait Fixture extends TestSupport {
		val academicYear = AcademicYear(2013)
		val monitoringPoint1 = new MonitoringPoint
		monitoringPoint1.name = "Point 1"
		monitoringPoint1.week = 5
		val monitoringPoint2 = new MonitoringPoint
		monitoringPoint2.name = "Point 2"
		monitoringPoint2.week = 15
		val monitoringPoints = List(monitoringPoint1, monitoringPoint2)

		val week5StartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1)
		val week5EndDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 8)
		val week15StartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.DECEMBER, 1)
		val week15EndDate = new DateMidnight(academicYear.startYear, DateTimeConstants.DECEMBER, 8)

		val route = mock[Route]
		val yearOption = Option(1)

		val week5pair = uk.ac.warwick.util.collections.Pair.of(new Integer(5), new Interval(week5StartDate, week5EndDate))
		val week15pair = uk.ac.warwick.util.collections.Pair.of(new Integer(15), new Interval(week15StartDate, week15EndDate))
		val weeksForYear = JArrayList(week5pair, week15pair)
		termService.getAcademicWeeksForYear(new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1))	returns weeksForYear

		val autumnTerm = mock[Term]
		autumnTerm.getTermTypeAsString returns "Autumn"
		val christmasVacation = mock[Vacation]
		christmasVacation.getTermTypeAsString returns "Christmas vacation"
		termService.getTermFromDateIncludingVacations(week5StartDate.withDayOfWeek(DayOfWeek.Thursday.getAsInt)) returns autumnTerm
		termService.getTermFromDateIncludingVacations(week15StartDate.withDayOfWeek(DayOfWeek.Thursday.getAsInt)) returns christmasVacation
	}

	@Test
	def commandApply() {
		new Fixture with GroupMonitoringPointsByTerm {
			val pointsByTerm = groupByTerm(monitoringPoints, academicYear)

			pointsByTerm("Autumn").size should be (1)
			pointsByTerm("Autumn").head.name should be (monitoringPoint1.name)
			pointsByTerm("Christmas vacation").size should be (1)
			pointsByTerm("Christmas vacation").head.name should be (monitoringPoint2.name)
		}
	}

}
