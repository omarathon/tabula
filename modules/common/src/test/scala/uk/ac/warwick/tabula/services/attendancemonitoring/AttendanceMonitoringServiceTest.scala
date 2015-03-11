package uk.ac.warwick.tabula.services.attendancemonitoring

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpointTotal, AttendanceMonitoringScheme, AttendanceState}
import uk.ac.warwick.tabula.data.{AttendanceMonitoringDao, AttendanceMonitoringDaoComponent}
import uk.ac.warwick.tabula.services.{TermService, TermServiceComponent, UserGroupMembershipHelper, UserLookupComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, MockUserLookup, Mockito, TestBase}

class AttendanceMonitoringServiceTest extends TestBase with Mockito {

	trait ServiceTestSupport extends AttendanceMonitoringDaoComponent with TermServiceComponent
		with UserLookupComponent with AttendanceMonitoringMembershipHelpers {
		val attendanceMonitoringDao = smartMock[AttendanceMonitoringDao]
		val termService = smartMock[TermService]
		val userLookup = new MockUserLookup
		val membersHelper = smartMock[UserGroupMembershipHelper[AttendanceMonitoringScheme]]
	}

	trait CheckpointFixture {
		val service = new AbstractAttendanceMonitoringService with ServiceTestSupport

		val department = Fixtures.department("it")

		val uniId1 = "1234"
		val member1 = Fixtures.student(uniId1)
		val uniId2 = "2345"
		val member2 = Fixtures.student(uniId2)

		val scheme = new AttendanceMonitoringScheme
		scheme.department = department
		scheme.academicYear = AcademicYear(2014)
		service.membersHelper.findBy(MemberOrUser(member1).asUser) returns Seq(scheme)
		service.membersHelper.findBy(MemberOrUser(member2).asUser) returns Seq(scheme)

		val point1 = Fixtures.attendanceMonitoringPoint(scheme, "point1", 2, 2)
		val point2 = Fixtures.attendanceMonitoringPoint(scheme, "point2", 4, 4)
		val point3 = Fixtures.attendanceMonitoringPoint(scheme, "point3", 4, 4)
		scheme.points = JArrayList(point1, point2, point3)
		val passedCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, member1, AttendanceState.fromCode("attended"))
		val missedCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, member1, AttendanceState.fromCode("unauthorised"))
		val authorisedCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point3, member1, AttendanceState.fromCode("authorised"))
	}

	@Test
	def setAttendance() { new CheckpointFixture { withUser("cusfal") {
		member1.mostSignificantCourse.beginDate = AcademicYear(2014).dateInTermOne.minusMonths(1).toLocalDate

		service.attendanceMonitoringDao.getCheckpoints(Seq(point1, point2, point3), member1) returns
			Map(point1 -> passedCheckpoint, point2 -> missedCheckpoint, point3 -> authorisedCheckpoint)
		service.attendanceMonitoringDao.getCheckpoints(Seq(point1, point2, point3), member1, withFlush = true) returns
			Map(point1 -> passedCheckpoint, point2 -> missedCheckpoint, point3 -> authorisedCheckpoint)
		service.attendanceMonitoringDao.getCheckpointTotal(member1, Option(department), AcademicYear(2014)) returns None

		val result = service.setAttendance(
			member1,
			Map(point1 -> AttendanceState.Attended, point2 -> AttendanceState.Attended, point3 -> null),
			currentUser
		)
		result.size should be (1)
		result.head should be (missedCheckpoint)
		verify(service.attendanceMonitoringDao, times(1)).removeCheckpoints(Seq(authorisedCheckpoint))
		verify(service.attendanceMonitoringDao, times(1)).saveOrUpdateCheckpoints(Seq(missedCheckpoint))
		verify(service.attendanceMonitoringDao, times(1)).saveOrUpdate(any[AttendanceMonitoringCheckpointTotal])
	}}}

	@Test
	def updateCheckpointTotal() { new CheckpointFixture { withUser("cusfal") {
		member1.mostSignificantCourse.beginDate = AcademicYear(2014).dateInTermOne.minusMonths(1).toLocalDate

		service.attendanceMonitoringDao.getCheckpoints(Seq(point1, point2, point3), member1) returns
			Map(point1 -> passedCheckpoint, point2 -> missedCheckpoint, point3 -> authorisedCheckpoint)
		service.attendanceMonitoringDao.getCheckpoints(Seq(point1, point2, point3), member1, withFlush = true) returns
			Map(point1 -> passedCheckpoint, point2 -> missedCheckpoint, point3 -> authorisedCheckpoint)
		service.attendanceMonitoringDao.getCheckpointTotal(member1, Option(department), AcademicYear(2014)) returns None

		val result = service.updateCheckpointTotal(
			member1,
			department,
			AcademicYear(2014)
		)
		verify(service.attendanceMonitoringDao, times(1)).saveOrUpdate(result)
		result.attended should be (1)
		result.unauthorised should be (1)
		result.authorised should be (1)
		result.unrecorded should be (0)
	}}}

}
