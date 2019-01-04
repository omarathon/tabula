package uk.ac.warwick.tabula.commands.attendance.view

import org.joda.time.{DateTime, LocalDate}
import org.mockito.Matchers
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.data.{ScalaOrder, ScalaRestriction}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.termdates.AcademicYearPeriod.PeriodType

class ReportStudentsChoosePeriodCommandTest extends TestBase with Mockito {

	trait TestSupport extends AttendanceMonitoringServiceComponent with ProfileServiceComponent {
		val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val profileService: ProfileService = smartMock[ProfileService]
	}

	trait Fixture {
		val student1: StudentMember = Fixtures.student("1234", "1234")
		student1.lastName = "Smith"
		student1.firstName = "Fred"
		val student2: StudentMember = Fixtures.student("2345", "2345")
		student2.lastName = "Smith"
		student2.firstName = "Bob"

		val currentUser: User = Fixtures.user("1574575", "u1574575")

		val point1: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(null)
		point1.startDate = new LocalDate(2013, 10, 1)
		val point2: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(null)
		point2.startDate = new LocalDate(2014, 1, 1)

		val fakeNow: DateTime = new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay

		val state = new ReportStudentsChoosePeriodCommandState with TestSupport {
			val department: Department = Fixtures.department("its")
			val academicYear = AcademicYear(2013)
		}

		val validator = new ReportStudentsChoosePeriodValidation with ReportStudentsChoosePeriodCommandState with TestSupport {
			val department: Department = Fixtures.department("its")
			val academicYear = AcademicYear(2013)
		}

		val command = new ReportStudentsChoosePeriodCommandInternal(Fixtures.department("its"),  AcademicYear(2013), currentUser) with ReportStudentsChoosePeriodCommandState with TestSupport
	}

	@Test
	def allStudents() { new Fixture {
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq()
		state.allStudents
		verify(state.profileService, times(1)).findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]])
	}}

	@Test
	def studentPointMap() { new Fixture {
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1, point2)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1, point2)
		val result: Map[StudentMember, Seq[AttendanceMonitoringPoint]] = state.studentPointMap
		result(student1) should be (Seq(point1, point2))
		result(student2) should be (Seq(point1, point2))
	}}

	@Test
	def termPoints() { new Fixture {
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1, point2)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1, point2)
		val result: Map[String, Seq[AttendanceMonitoringPoint]] = state.termPoints
		result(PeriodType.autumnTerm.toString) should be (Seq(point1))
		result(PeriodType.christmasVacation.toString) should be (Seq(point2))
	}}

	@Test
	def studentReportCounts() { new Fixture {
		state.period = PeriodType.autumnTerm.toString
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1)
		state.attendanceMonitoringService.findNonReportedTerms(state.allStudents, state.academicYear) returns Seq(PeriodType.autumnTerm.toString)
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq()

		val student1point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student1, AttendanceState.MissedUnauthorised)
		val student2point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, student2, AttendanceState.MissedUnauthorised)
		state.attendanceMonitoringService.getCheckpoints(Seq(point1), state.allStudents) returns
			Map(student1 -> Map(point1 -> student1point1missed),
			    student2 -> Map(point1 -> student2point1missed))

		val result: Seq[StudentReportCount] = state.studentReportCounts
		result.size should be (2)
	}}

	@Test
	def studentReportCountsUnreportedOnly() { new Fixture {
		state.period = PeriodType.autumnTerm.toString
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1)
		state.attendanceMonitoringService.findNonReportedTerms(state.allStudents, state.academicYear) returns Seq(PeriodType.autumnTerm.toString)
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq()

		val student1point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student1, AttendanceState.MissedUnauthorised)
		val student2point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, student2, AttendanceState.MissedUnauthorised)
		state.attendanceMonitoringService.getCheckpoints(Seq(point1), state.allStudents) returns
			Map(student1 -> Map(point1 -> student1point1missed),
				student2 -> Map(point1 -> student2point1missed))

		state.attendanceMonitoringService.studentAlreadyReportedThisTerm(student1, point1) returns true
		state.attendanceMonitoringService.studentAlreadyReportedThisTerm(student2, point1) returns false

		val result: Seq[StudentReportCount] = state.studentMissedReportCounts
		result.size should be (1)
		result.head.student should be (student2)
	}}

	@Test
	def availablePeriods() { new Fixture { withFakeTime(fakeNow) {
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1, point2)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1, point2)
		state.attendanceMonitoringService.findNonReportedTerms(state.allStudents, state.academicYear) returns Seq(PeriodType.autumnTerm.toString)
		val result = state.availablePeriods
		result.size should be (2)
		result.contains((PeriodType.autumnTerm.toString, true)) should be {true}
		result.contains((PeriodType.autumnTerm.toString, false)) should be {false}
		result.contains((PeriodType.christmasVacation.toString, false)) should be {true}
		result.contains((PeriodType.christmasVacation.toString, true)) should be {false}
	}}}

	trait ValidatorFixture extends Fixture {
		validator.profileService.findAllStudentsByRestrictions(Matchers.eq(validator.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		validator.attendanceMonitoringService.listStudentsPoints(student1, Option(validator.department), validator.academicYear) returns Seq(point1, point2)
		validator.attendanceMonitoringService.listStudentsPoints(student2, Option(validator.department), validator.academicYear) returns Seq(point1, point2)
		validator.attendanceMonitoringService.findNonReportedTerms(validator.allStudents, validator.academicYear) returns Seq(PeriodType.autumnTerm.toString)
	}

	@Test
	def validation() {
		new ValidatorFixture { withFakeTime(fakeNow) {
			val errors = new BindException(validator, "command")
			validator.period = PeriodType.springTerm.toString
			validator.validate(errors)
			errors.getAllErrors.size should be (1)
		}}
		new ValidatorFixture { withFakeTime(fakeNow) {
			val errors = new BindException(validator, "command")
			validator.period = PeriodType.christmasVacation.toString
			validator.validate(errors)
			errors.getAllErrors.size should be (1)
		}}
		new ValidatorFixture { withFakeTime(fakeNow) {
			val errors = new BindException(validator, "command")
			validator.period = PeriodType.autumnTerm.toString
			validator.validate(errors)
			errors.getAllErrors.size should be (0)
		}}
	}

	trait CommandFixture extends Fixture {
		command.profileService.findAllStudentsByRestrictions(Matchers.eq(command.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		command.attendanceMonitoringService.listStudentsPoints(student1, Option(command.department), command.academicYear) returns Seq(point1, point2)
		command.attendanceMonitoringService.listStudentsPoints(student2, Option(command.department), command.academicYear) returns Seq(point1, point2)
		command.attendanceMonitoringService.findNonReportedTerms(command.allStudents, command.academicYear) returns Seq(PeriodType.autumnTerm.toString)
	}

	@Test
	def apply() { new CommandFixture {
		val student1point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student1, AttendanceState.MissedUnauthorised)
		val student1point2missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, student1, AttendanceState.MissedUnauthorised)
		command.period = PeriodType.autumnTerm.toString
		command.attendanceMonitoringService.getCheckpoints(Seq(point1), command.allStudents) returns Map(student1 -> Map(point1 -> student1point1missed, point2 -> student1point2missed))
		val result: StudentReport = command.applyInternal()
		result.studentReportCounts.size should be (1)
		result.studentReportCounts.head.missed should be (1)
	}}
}
