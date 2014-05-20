package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.{Fixtures, CurrentUser, Mockito, TestBase}
import uk.ac.warwick.tabula.services.{AttendanceMonitoringService, ProfileService, ModuleAndDepartmentService, CourseAndRouteService, AttendanceMonitoringServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.data.SchemeMembershipItem

class FindStudentsForSchemeCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ProfileServiceComponent
		with FindStudentsForSchemeCommandState with AttendanceMonitoringServiceComponent {

		val courseAndRouteService = smartMock[CourseAndRouteService]
		val moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		val profileService = smartMock[ProfileService]
		val attendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val sitsStatusDao = smartMock[SitsStatusDao]
		val modeOfAttendanceDao = smartMock[ModeOfAttendanceDao]
		def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route] = {
			Set()
		}

	}

	trait Fixture {
		val scheme = new AttendanceMonitoringScheme
		scheme.department = new Department
		val student1 = Fixtures.student("1234")
		val student2 = Fixtures.student("2345")
		val student3 = Fixtures.student("3456")
	}

	@Test
	def apply() { withUser("cusfal") { new Fixture {
		val command = new FindStudentsForSchemeCommandInternal(scheme, currentUser) with CommandTestSupport

		command.routes.add(new Route(){ this.code = "a100" })

		command.profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
			any[Department], any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]
		) returns Seq(student1.universityId, student2.universityId)

		command.attendanceMonitoringService.findSchemeMembershipItems(
			Seq(student1.universityId, student2.universityId), SchemeMembershipStaticType
		) returns Seq(
			SchemeMembershipItem(SchemeMembershipStaticType, student1.firstName, student1.lastName, student1.universityId, student1.userId, Seq()),
			SchemeMembershipItem(SchemeMembershipStaticType, student2.firstName, student2.lastName, student2.universityId, student2.userId, Seq())
		)

		command.updatedIncludedStudentIds.add(student3.universityId)
		command.updatedExcludedStudentIds.add(student2.universityId)

		val result = command.applyInternal()
		// 2 results from search, even with 1 removed
		result.membershipItems.size should be (2)
		// 1 marked static
		result.membershipItems.count(_.itemType == SchemeMembershipStaticType) should be (1)
		// 1 marked removed
		result.membershipItems.count(_.itemType == SchemeMembershipExcludeType) should be (1)
		// 0 marked included (not displayed if not in search)
		result.membershipItems.count(_.itemType == SchemeMembershipIncludeType) should be (0)

		result.membershipItems.size should be (result.updatedStaticStudentIds.size)
	}}}

}
