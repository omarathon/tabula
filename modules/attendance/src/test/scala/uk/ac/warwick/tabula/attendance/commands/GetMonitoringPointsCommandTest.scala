package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.data.model.Route
import scala.collection.JavaConverters._
import org.joda.time.{Interval, DateTimeConstants, DateMidnight}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.util.termdates.Term
import scala.Predef._
import uk.ac.warwick.tabula.services.Vacation

class GetMonitoringPointsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends RouteServiceComponent with TermServiceComponent {
		val routeService = mock[RouteService]
		val termService = mock[TermService]

		def apply(): Option[Pair[MonitoringPointSet, Map[String, Seq[MonitoringPoint]]]] = {
			null
		}
	}

	trait Fixture {
		val monitoringPointSet = new MonitoringPointSet
		monitoringPointSet.academicYear = AcademicYear(2013)
		val monitoringPoint1 = new MonitoringPoint
		monitoringPoint1.name = "Point 1"
		monitoringPoint1.week = 5
		val monitoringPoint2 = new MonitoringPoint
		monitoringPoint2.name = "Point 2"
		monitoringPoint2.week = 15
		monitoringPointSet.points = List(monitoringPoint1, monitoringPoint2).asJava

		val week5StartDate = new DateMidnight(monitoringPointSet.academicYear.startYear, DateTimeConstants.NOVEMBER, 1)
		val week5EndDate = new DateMidnight(monitoringPointSet.academicYear.startYear, DateTimeConstants.NOVEMBER, 8)
		val week15StartDate = new DateMidnight(monitoringPointSet.academicYear.startYear, DateTimeConstants.DECEMBER, 1)
		val week15EndDate = new DateMidnight(monitoringPointSet.academicYear.startYear, DateTimeConstants.DECEMBER, 8)

		val route = mock[Route]
		val yearOption = Option(1)

		val command = new GetMonitoringPointsCommand(route, yearOption) with CommandTestSupport

		command.routeService.findMonitoringPointSet(route, yearOption) returns Option(monitoringPointSet)

		val week5pair = uk.ac.warwick.util.collections.Pair.of(new Integer(5), new Interval(week5StartDate, week5EndDate))
		val week15pair = uk.ac.warwick.util.collections.Pair.of(new Integer(15), new Interval(week15StartDate, week15EndDate))
		val weeksForYear = JArrayList(week5pair, week15pair)
		command.termService.getAcademicWeeksForYear(new DateMidnight(monitoringPointSet.academicYear.startYear, DateTimeConstants.NOVEMBER, 1))	returns weeksForYear

		val autumnTerm = mock[Term]
		autumnTerm.getTermTypeAsString returns "Autumn"
		val christmasVacation = mock[Vacation]
		christmasVacation.getTermTypeAsString returns "Christmas vacation"
		command.termService.getTermFromDateIncludingVacations(week5StartDate.withDayOfWeek(1)) returns autumnTerm
		command.termService.getTermFromDateIncludingVacations(week15StartDate.withDayOfWeek(1)) returns christmasVacation
	}

	@Test
	def commandApply() {
		new Fixture {
			val pointSetPair = command.applyInternal().get
			val pointsByTerm = pointSetPair._2

			pointsByTerm("Autumn").size should be (1)
			pointsByTerm("Autumn").head.name should be (monitoringPoint1.name)
			pointsByTerm("Christmas vacation").size should be (1)
			pointsByTerm("Christmas vacation").head.name should be (monitoringPoint2.name)
		}
	}

}
