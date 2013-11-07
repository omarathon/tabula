package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{Fixtures, AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.Route
import org.joda.time.{Interval, DateTimeConstants, DateMidnight}
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
		monitoringPoint1.validFromWeek = 5
		val monitoringPoint2 = new MonitoringPoint
		monitoringPoint2.name = "Point 2"
		monitoringPoint2.validFromWeek = 15
		val monitoringPoints = List(monitoringPoint1, monitoringPoint2)

		val week5StartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1)
		val week5EndDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 8)
		val week15StartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.DECEMBER, 1)
		val week15EndDate = new DateMidnight(academicYear.startYear, DateTimeConstants.DECEMBER, 8)

		val week5pair = (new Integer(5), new Interval(week5StartDate, week5EndDate))
		val week15pair = (new Integer(15), new Interval(week15StartDate, week15EndDate))
		val weeksForYear = Seq(week5pair, week15pair)
		termService.getAcademicWeeksForYear(new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1))	returns weeksForYear

		val autumnTerm = mock[Term]
		autumnTerm.getTermTypeAsString returns "Autumn"
		val christmasVacation = mock[Vacation]
		christmasVacation.getTermTypeAsString returns "Christmas vacation"
		termService.getTermFromDateIncludingVacations(week5StartDate.withDayOfWeek(DayOfWeek.Thursday.getAsInt)) returns autumnTerm
		termService.getTermFromDateIncludingVacations(week15StartDate.withDayOfWeek(DayOfWeek.Thursday.getAsInt)) returns christmasVacation
	}

	@Test
	def pointsByTerm() {
		new Fixture with GroupMonitoringPointsByTerm {
			val pointsByTerm = groupByTerm(monitoringPoints, academicYear)

			pointsByTerm("Autumn").size should be (1)
			pointsByTerm("Autumn").head.name should be (monitoringPoint1.name)
			pointsByTerm("Christmas vacation").size should be (1)
			pointsByTerm("Christmas vacation").head.name should be (monitoringPoint2.name)
		}
	}

	trait GroupedPointsFixture extends TestSupport {
		val academicYear = AcademicYear(2013)

		val route1 = Fixtures.route("test1")
		val route2 = Fixtures.route("test2")
		val route3 = Fixtures.route("test3")
		val route4 = Fixtures.route("test4")
		val pointSet1 = new MonitoringPointSet
		pointSet1.route = route1
		val pointSet2 = new MonitoringPointSet
		pointSet2.route = route2
		val pointSet3 = new MonitoringPointSet
		pointSet3.route = route3
		val pointSet4 = new MonitoringPointSet
		pointSet4.route = route4

		val commonPoint1 = Fixtures.monitoringPoint("Common name", 1, 2)
		commonPoint1.pointSet = pointSet1
		val commonPoint2 = Fixtures.monitoringPoint("Common name", 1, 2)
		commonPoint2.pointSet = pointSet2
		val differentCaseButCommonPoint = Fixtures.monitoringPoint("common name", 1, 2)
		differentCaseButCommonPoint.pointSet = pointSet3
		val sameNameButDifferentWeekPoint = Fixtures.monitoringPoint("Common name", 1, 1)
		sameNameButDifferentWeekPoint.pointSet = pointSet4
		val duplicatePoint = Fixtures.monitoringPoint("Common name", 1, 2)
		duplicatePoint.pointSet = pointSet1
		val monitoringPoints = List(commonPoint1, commonPoint2, differentCaseButCommonPoint, sameNameButDifferentWeekPoint, duplicatePoint)

		val week1StartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1)
		val week1EndDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 8)
		val week2StartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 15)
		val week2EndDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 22)
		val week1pair = (new Integer(1), new Interval(week1StartDate, week2EndDate))
		val week2pair = (new Integer(2), new Interval(week2StartDate, week2EndDate))
		val weeksForYear = Seq(week1pair, week2pair)
		termService.getAcademicWeeksForYear(new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1))	returns weeksForYear
		val autumnTerm = mock[Term]
		autumnTerm.getTermTypeAsString returns "Autumn"
		termService.getTermFromDateIncludingVacations(week1StartDate.withDayOfWeek(DayOfWeek.Thursday.getAsInt)) returns autumnTerm
		termService.getTermFromDateIncludingVacations(week2StartDate.withDayOfWeek(DayOfWeek.Thursday.getAsInt)) returns autumnTerm
	}

	@Test
	def groupedPointsByTerm() {
		new GroupedPointsFixture with GroupMonitoringPointsByTerm {
			val groupedPointsByTerm = groupSimilarPointsByTerm(monitoringPoints, Seq(), academicYear)

			val autumnPoints = groupedPointsByTerm("Autumn")
			autumnPoints.size should be (2)
			val groupedPoint = autumnPoints(1)
			groupedPoint should not be null
			groupedPoint.routes.size should be (3)

		}
	}

}
