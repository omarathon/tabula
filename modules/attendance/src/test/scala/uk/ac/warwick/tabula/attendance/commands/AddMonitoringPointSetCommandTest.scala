package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Route, Department}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._

class AddMonitoringPointSetCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends RouteServiceComponent with TermServiceComponent
			with AddMonitoringPointSetValidation with AddMonitoringPointSetState {
		val routeService = mock[RouteService]
		val termService = mock[TermService]
	}

	trait Fixture {
		val dept = new Department()
		val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
		val lastAcademicYear = AcademicYear.guessByDate(new DateTime()).previous

		val monitoringPoint = new MonitoringPoint
		val existingName = "Point 1"
		val existingWeek = 1
		monitoringPoint.name = existingName
		monitoringPoint.week = existingWeek

		val routeWithAllYears = new Route
		val routeWithAllYearsCode = "a101"
		routeWithAllYears.code = routeWithAllYearsCode
		val allYearsMPS = new MonitoringPointSet()
		allYearsMPS.year = null
		allYearsMPS.academicYear = thisAcademicYear
		routeWithAllYears.monitoringPointSets = JArrayList(allYearsMPS)

		val routeWithOneYear = new Route
		val routeWithOneYearCode = "b101"
		routeWithOneYear.code = routeWithOneYearCode
		val oneYearMPS = new MonitoringPointSet()
		oneYearMPS.year = 1
		oneYearMPS.academicYear = thisAcademicYear
		routeWithOneYear.monitoringPointSets = JArrayList(oneYearMPS)

		val emptyRoute = new Route
		val emptyRouteCode = "c101"
		emptyRoute.code = emptyRouteCode

		dept.routes = JArrayList(routeWithAllYears, routeWithOneYear, emptyRoute)
		val command = new AddMonitoringPointSetCommand(dept, null) with CommandTestSupport
	}

	@Test
	def validateNoYears() {
		new Fixture {
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
		}
	}

	@Test
	def validateNoPoints() {
		new Fixture {
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("monitoringPoints").size() should be (1)
		}
	}

	@Test
	def validateAllYearsExists() {
		new Fixture {
			command.selectedRoutesAndYears.get(routeWithAllYears).put("All", true)
			command.academicYear = thisAcademicYear
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
			errors.getFieldErrors("selectedRoutesAndYears").get(0).getCode should be ("monitoringPointSet.allYears")
		}
	}

	@Test
	def validateAllYearsExistsDifferentAcademicYear() {
		new Fixture {
			command.selectedRoutesAndYears.get(routeWithAllYears).put("All", true)
			command.academicYear = lastAcademicYear
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (0)
		}
	}

	@Test
	def validateOneYearExistsDuplicate() {
		new Fixture {
			command.selectedRoutesAndYears.get(routeWithOneYear).put("1", true)
			command.academicYear = thisAcademicYear
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
			errors.getFieldErrors("selectedRoutesAndYears").get(0).getCode should be ("monitoringPointSet.duplicate")
		}
	}

	@Test
	def validateOneYearExistsDuplicateDifferentAcademicYear() {
		new Fixture {
			command.selectedRoutesAndYears.get(routeWithOneYear).put("1", true)
			command.academicYear = lastAcademicYear
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (0)
		}
	}

	@Test
	def validateOneYearExistsAllYearsSet() {
		new Fixture {
			command.selectedRoutesAndYears.get(routeWithOneYear).put("All", true)
			command.academicYear = thisAcademicYear
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
			errors.getFieldErrors("selectedRoutesAndYears").get(0).getCode should be ("monitoringPointSet.alreadyYear")
		}
	}

	@Test
	def validateOneYearExistsAllYearsSetDifferentAcademicYear() {
		new Fixture {
			command.selectedRoutesAndYears.get(routeWithOneYear).put("All", true)
			command.academicYear = lastAcademicYear
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (0)
		}
	}

	@Test
	def validateMixedYears() {
		new Fixture {
			command.selectedRoutesAndYears.get(emptyRoute).put("1", true)
			command.selectedRoutesAndYears.get(emptyRoute).put("All", true)
			command.academicYear = thisAcademicYear
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
			errors.getFieldErrors("selectedRoutesAndYears").get(0).getCode should be ("monitoringPointSet.mixed")
		}
	}

	@Test
	def validatePointsNoNameNoWeek() {
		new Fixture {
			command.selectedRoutesAndYears.get(emptyRoute).put("1", true)
			command.academicYear = thisAcademicYear
			val newPoint = new MonitoringPoint
			command.monitoringPoints.add(newPoint)
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("monitoringPoints[0].name").size() should be (1)
			errors.getFieldErrors("monitoringPoints[0].week").size() should be (1)
		}
	}

	@Test
	def validatePointsDuplicate() {
		new Fixture {
			command.selectedRoutesAndYears.get(emptyRoute).put("1", true)
			command.academicYear = thisAcademicYear
			command.monitoringPoints.add(monitoringPoint)
			val newPoint = new MonitoringPoint
			newPoint.name = existingName
			newPoint.week = existingWeek
			command.monitoringPoints.add(newPoint)
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("monitoringPoints[0].name").size() should be (1)
			errors.getFieldErrors("monitoringPoints[1].name").size() should be (1)
		}
	}
}
