package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AttendanceMonitoringService}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, FeaturesComponent, Mockito, TestBase}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.commands.{DeserializesFilter, TaskBenchmarking}
import uk.ac.warwick.tabula.data.{ScalaOrder, ScalaRestriction}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme

class UpdateAttendanceMonitoringSchemeMembershipCommandTest extends TestBase with Mockito {

	var deserializeFilterCalled = false

	trait CommandTestSupport extends FeaturesComponent with AttendanceMonitoringServiceComponent
		with DeserializesFilter	with UpdateAttendanceMonitoringSchemeMembershipCommandState with TaskBenchmarking {

		val features = emptyFeatures
		val attendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val profileService = smartMock[ProfileService]
		profileService.getAllMembersWithUniversityIds(Seq()) returns Seq()
		def deserializeFilter(filterString: String) = {
			deserializeFilterCalled = true
		}
	}

	trait Fixture {
		deserializeFilterCalled = false
		val cmd = new UpdateAttendanceMonitoringSchemeMembershipCommandInternal with CommandTestSupport
	}

	trait FeatureEnabledFixture extends Fixture {
		cmd.features.attendanceMonitoringAcademicYear2014 = true
	}

	@Test
	def featureTest() { new Fixture {
		cmd.features.attendanceMonitoringAcademicYear2014 = false
		cmd.applyInternal()
		verify(cmd.attendanceMonitoringService, times(0)).listSchemesForMembershipUpdate
		deserializeFilterCalled should be {false}
	}}

	@Test
	def noSchemes() { new FeatureEnabledFixture {
		cmd.attendanceMonitoringService.listSchemesForMembershipUpdate returns Seq()
		val schemes = cmd.applyInternal()
		deserializeFilterCalled should be {false}
		schemes.isEmpty should be {true}
	}}

	@Test
	def sameStudentInMultipleSchemesInSameDept() { new FeatureEnabledFixture {
		val dept = Fixtures.department("its")
		val scheme1 = new AttendanceMonitoringScheme
		scheme1.attendanceMonitoringService = None
		scheme1.department = dept
		scheme1.academicYear = AcademicYear(2014)

		val scheme2 = new AttendanceMonitoringScheme
		scheme2.attendanceMonitoringService = None
		scheme2.department = dept
		scheme2.academicYear = AcademicYear(2014)
		cmd.attendanceMonitoringService.listSchemesForMembershipUpdate returns Seq(scheme1, scheme2)

		val student = Fixtures.student("1234")
		cmd.profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(isEq(dept), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student.universityId)
		cmd.profileService.getAllMembersWithUniversityIds(Seq(student.universityId)) returns Seq(student)

		val schemes = cmd.applyInternal()
		deserializeFilterCalled should be {true}
		verify(cmd.attendanceMonitoringService, times(1)).saveOrUpdate(scheme1)
		verify(cmd.attendanceMonitoringService, times(1)).saveOrUpdate(scheme2)
		schemes.contains(scheme1) should be {true}
		schemes.contains(scheme2) should be {true}
		verify(cmd.attendanceMonitoringService, times(1)).updateCheckpointTotal(student, dept, AcademicYear(2014))
	}}

	@Test
	def sameStudentInMultipleSchemesInDifferentDept() { new FeatureEnabledFixture {
		val dept1 = Fixtures.department("its")
		val dept2 = Fixtures.department("foo")
		val scheme1 = new AttendanceMonitoringScheme
		scheme1.attendanceMonitoringService = None
		scheme1.department = dept1
		scheme1.academicYear = AcademicYear(2014)

		val scheme2 = new AttendanceMonitoringScheme
		scheme2.attendanceMonitoringService = None
		scheme2.department = dept2
		scheme2.academicYear = AcademicYear(2014)

		cmd.attendanceMonitoringService.listSchemesForMembershipUpdate returns Seq(scheme1, scheme2)

		val student = Fixtures.student("1234")
		cmd.profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(isEq(dept1), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student.universityId)
		cmd.profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(isEq(dept2), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student.universityId)
		cmd.profileService.getAllMembersWithUniversityIds(Seq(student.universityId)) returns Seq(student)

		val schemes = cmd.applyInternal()
		deserializeFilterCalled should be {true}
		verify(cmd.attendanceMonitoringService, times(1)).saveOrUpdate(scheme1)
		verify(cmd.attendanceMonitoringService, times(1)).saveOrUpdate(scheme2)
		schemes.contains(scheme1) should be {true}
		schemes.contains(scheme2) should be {true}
		verify(cmd.attendanceMonitoringService, times(1)).updateCheckpointTotal(student, dept1, AcademicYear(2014))
		verify(cmd.attendanceMonitoringService, times(1)).updateCheckpointTotal(student, dept2, AcademicYear(2014))
	}}

}
