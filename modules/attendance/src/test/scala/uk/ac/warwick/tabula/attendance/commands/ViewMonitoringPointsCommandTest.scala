package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{CurrentUser, AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services.{MonitoringPointService, ProfileService, TermService, MonitoringPointServiceComponent, ProfileServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.data.model.{Route, Department}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.{ScalaOrder, ScalaRestriction}
import org.mockito.Matchers

class ViewMonitoringPointsCommandTest extends TestBase with Mockito {
	
	trait CommandTestSupport extends ViewMonitoringPointsState
	with TermServiceComponent with ProfileServiceComponent with MonitoringPointServiceComponent {
		val termService = mock[TermService]
		val profileService = mock[ProfileService]
		val monitoringPointService = mock[MonitoringPointService]
	}
	
	trait Fixture {
		val thisAcademicYear = AcademicYear(2013)
		val user = mock[CurrentUser]

		val sprF = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
		val sprP = Fixtures.sitsStatus("P", "Permanently Withdrawn", "Permanently Withdrawn")

		val moaFT = Fixtures.modeOfAttendance("F", "FT", "Full time")
		val moaPT = Fixtures.modeOfAttendance("P", "PT", "Part time")
		
		val dept = Fixtures.department("arc", "School of Architecture")

		val route1 = Fixtures.route("a501", "Architecture BA")
		val route2 = Fixtures.route("a502", "Architecture BA with Intercalated year")
		val route3 = Fixtures.route("a9p1", "Architecture MA")
		dept.routes.add(route1)
		dept.routes.add(route2)
		dept.routes.add(route3)
	}

	trait DeptAdminCommand extends Fixture {
		val command = new ViewMonitoringPointsCommand(dept, Option(thisAcademicYear), user) with CommandTestSupport {
			def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route]  ={
				dept.routes.asScala.toSet
			}
		}
		command.profileService.allSprStatuses(dept) returns Seq(sprF, sprP)
		command.profileService.allModesOfAttendance(dept) returns Seq(moaFT, moaPT)
	}

	// The route manager can only manage A501 (route1)
	trait RouteManagerCommand extends Fixture {
		val command = new ViewMonitoringPointsCommand(dept, Option(thisAcademicYear), user) with CommandTestSupport {
			def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route]  ={
				Set(route1)
			}
		}
		command.profileService.allSprStatuses(dept) returns Seq(sprF, sprP)
		command.profileService.allModesOfAttendance(dept) returns Seq(moaFT, moaPT)
	}
	
	@Test
	def onBindCanSeeAllRoutesNoFilter() { new DeptAdminCommand {
		command.onBind(null)
		command.routes.size() should be (0)
	}}

	@Test
	def onBindCanSeeAllRoutesWithFilter() { new DeptAdminCommand {
		command.routes.add(route1)
		command.onBind(null)
		command.routes.size() should be (1)
		command.routes.get(0) should be (route1)
	}}

	@Test
	def onBindCanSee1RouteNoFilter() { new RouteManagerCommand {
		command.onBind(null)
		command.routes.size() should be (1)
		command.routes.get(0) should be (route1)
	}}

	@Test
	def onBindCanSee1RouteWithFilterRouteAllowed() { new RouteManagerCommand {
		command.routes.add(route1)
		command.onBind(null)
		command.routes.size() should be (1)
		command.routes.get(0) should be (route1)
	}}

	@Test
	def onBindCanSee1RouteWithFilterRouteNotAllowed() { new RouteManagerCommand {
		command.routes.add(route1)
		command.routes.add(route2)
		command.onBind(null)
		command.routes.size() should be (1)
		command.routes.get(0) should be (route1)
	}}

	@Test
	def onBindStudentsPopulated() { new RouteManagerCommand {
		command.onBind(null)
		there was one (command.profileService).findAllStudentsByRestrictions(Matchers.eq(dept), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]])
	}}

}
