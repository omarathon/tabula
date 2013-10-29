package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Route, Department}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.NoCurrentUser
import scala.collection.JavaConverters._

class AddMonitoringPointSetCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent with MonitoringPointServiceComponent
			with AddMonitoringPointSetValidation with AddMonitoringPointSetState with PermissionsAwareRoutes {
		val courseAndRouteService = mock[CourseAndRouteService]
		val termService = mock[TermService]
		val monitoringPointService = mock[MonitoringPointService]
		def routesForPermission(user: CurrentUser, p: Permission, dept: Department) = dept.routes.asScala.toSet
	}

	trait Fixture {
		val dept = new Department()
		val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
		val lastAcademicYear = AcademicYear.guessByDate(new DateTime()).previous

		val monitoringPoint = new MonitoringPoint
		val existingName = "Point 1"
		val existingWeek = 1
		monitoringPoint.name = existingName
		monitoringPoint.validFromWeek = existingWeek
		monitoringPoint.requiredFromWeek = existingWeek

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
		val thisYearCommand = new AddMonitoringPointSetCommand(NoCurrentUser(), dept, thisAcademicYear, None) with CommandTestSupport
		val lastYearCommand = new AddMonitoringPointSetCommand(NoCurrentUser(), dept, lastAcademicYear, None) with CommandTestSupport
	}

	@Test
	def validateNoYears() {
		new Fixture {
			var errors = new BindException(thisYearCommand, "command")
			thisYearCommand.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
		}
	}

	@Test
	def validateNoPoints() {
		new Fixture {
			var errors = new BindException(thisYearCommand, "command")
			thisYearCommand.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("monitoringPoints").size() should be (1)
		}
	}

	@Test
	def validateAllYearsExists() {
		new Fixture {
			thisYearCommand.selectedRoutesAndYears.get(routeWithAllYears).put("All", true)
			var errors = new BindException(thisYearCommand, "command")
			thisYearCommand.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
			errors.getFieldErrors("selectedRoutesAndYears").get(0).getCode should be ("monitoringPointSet.allYears")
		}
	}

	@Test
	def validateAllYearsExistsDifferentAcademicYear() {
		new Fixture {
			lastYearCommand.selectedRoutesAndYears.get(routeWithAllYears).put("All", true)
			var errors = new BindException(lastYearCommand, "command")
			lastYearCommand.validate(errors)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (0)
		}
	}

	@Test
	def validateOneYearExistsDuplicate() {
		new Fixture {
			thisYearCommand.selectedRoutesAndYears.get(routeWithOneYear).put("1", true)
			var errors = new BindException(thisYearCommand, "command")
			thisYearCommand.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
			errors.getFieldErrors("selectedRoutesAndYears").get(0).getCode should be ("monitoringPointSet.duplicate")
		}
	}

	@Test
	def validateOneYearExistsDuplicateDifferentAcademicYear() {
		new Fixture {
			lastYearCommand.selectedRoutesAndYears.get(routeWithOneYear).put("1", true)
			var errors = new BindException(lastYearCommand, "command")
			lastYearCommand.validate(errors)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (0)
		}
	}

	@Test
	def validateOneYearExistsAllYearsSet() {
		new Fixture {
			thisYearCommand.selectedRoutesAndYears.get(routeWithOneYear).put("All", true)
			var errors = new BindException(thisYearCommand, "command")
			thisYearCommand.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
			errors.getFieldErrors("selectedRoutesAndYears").get(0).getCode should be ("monitoringPointSet.alreadyYear")
		}
	}

	@Test
	def validateOneYearExistsAllYearsSetDifferentAcademicYear() {
		new Fixture {
			lastYearCommand.selectedRoutesAndYears.get(routeWithOneYear).put("All", true)
			var errors = new BindException(lastYearCommand, "command")
			lastYearCommand.validate(errors)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (0)
		}
	}

	@Test
	def validateMixedYears() {
		new Fixture {
			thisYearCommand.selectedRoutesAndYears.get(emptyRoute).put("1", true)
			thisYearCommand.selectedRoutesAndYears.get(emptyRoute).put("All", true)
			var errors = new BindException(thisYearCommand, "command")
			thisYearCommand.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("selectedRoutesAndYears").size() should be (1)
			errors.getFieldErrors("selectedRoutesAndYears").get(0).getCode should be ("monitoringPointSet.mixed")
		}
	}

	@Test
	def validatePointsNoNameNoWeek() {
		new Fixture {
			thisYearCommand.selectedRoutesAndYears.get(emptyRoute).put("1", true)
			val newPoint = new MonitoringPoint
			thisYearCommand.monitoringPoints.add(newPoint)
			var errors = new BindException(thisYearCommand, "command")
			thisYearCommand.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("monitoringPoints[0].name").size() should be (1)
			errors.getFieldErrors("monitoringPoints[0].validFromWeek").size() should be (1)
		}
	}

	@Test
	def validatePointsDuplicate() {
		new Fixture {
			thisYearCommand.selectedRoutesAndYears.get(emptyRoute).put("1", true)
			thisYearCommand.monitoringPoints.add(monitoringPoint)
			val newPoint = new MonitoringPoint
			newPoint.name = existingName
			newPoint.validFromWeek = existingWeek
			newPoint.requiredFromWeek = existingWeek
			thisYearCommand.monitoringPoints.add(newPoint)
			var errors = new BindException(thisYearCommand, "command")
			thisYearCommand.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors("monitoringPoints[0].name").size() should be (1)
			errors.getFieldErrors("monitoringPoints[1].name").size() should be (1)
		}
	}
}
