package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AttendanceMonitoringService}
import uk.ac.warwick.tabula.{Fixtures, AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import org.springframework.validation.BindException
import org.joda.time.DateTime
import uk.ac.warwick.util.termdates.{TermImpl, Term}
import scala.collection.mutable
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.JavaImports.JHashSet
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringScheme, AttendanceMonitoringPoint, MonitoringPointReport}

class CreateAttendancePointCommandTest extends TestBase with Mockito {

	trait Fixture {
		val thisTermService = smartMock[TermService]
		val command = new CreateAttendancePointCommandState with TermServiceComponent with SmallGroupServiceComponent with ModuleAndDepartmentServiceComponent {
			val department = null
			val academicYear = null
			val schemes = null
			val termService = thisTermService
			val smallGroupService = null
			val moduleAndDepartmentService = null
		}
		val validator = new AttendanceMonitoringPointValidation with TermServiceComponent with AttendanceMonitoringServiceComponent {
			val termService = thisTermService
			val attendanceMonitoringService = smartMock[AttendanceMonitoringService]
		}
		val errors = new BindException(command, "command")
	}

	@Test
	def validateName() { new Fixture {
		validator.validateName(errors, "Name")
		errors.hasFieldErrors("name") should be (false)
		validator.validateName(errors, "")
		errors.hasFieldErrors("name") should be (true)
	}}

	@Test
	def validateWeek() { new Fixture {
		validator.validateWeek(errors, 1, "startWeek")
		errors.hasFieldErrors("startWeek") should be (false)
		validator.validateWeek(errors, 53, "startWeek")
		errors.hasFieldErrors("startWeek") should be (true)
	}}

	@Test
	def validateWeeks() { new Fixture {
		validator.validateWeeks(errors, 2, 3)
		errors.hasFieldErrors("startWeek") should be (false)
		validator.validateWeeks(errors, 2, 1)
		errors.hasFieldErrors("startWeek") should be (true)
	}}

	@Test
	def validateDate() {
		new Fixture {
			validator.validateDate(errors, null, null, "startDate")
			errors.hasFieldErrors("startDate") should be (true)
		}
		new Fixture {
			val date = new DateTime().withYear(2013).toLocalDate
			validator.termService.getAcademicWeekForAcademicYear(date.toDateTimeAtStartOfDay, AcademicYear(2014)) returns Term.WEEK_NUMBER_BEFORE_START
			validator.validateDate(errors, date, AcademicYear(2014), "startDate")
			errors.hasFieldErrors("startDate") should be (true)
		}
		new Fixture {
			val date = new DateTime().withYear(2016).toLocalDate
			validator.termService.getAcademicWeekForAcademicYear(date.toDateTimeAtStartOfDay, AcademicYear(2014)) returns Term.WEEK_NUMBER_AFTER_END
			validator.validateDate(errors, date, AcademicYear(2014), "startDate")
			errors.hasFieldErrors("startDate") should be (true)
		}
		new Fixture {
			val date = new DateTime().withYear(2015).toLocalDate
			validator.termService.getAcademicWeekForAcademicYear(date.toDateTimeAtStartOfDay, AcademicYear(2014)) returns 10
			validator.validateDate(errors, date, AcademicYear(2014), "startDate")
			errors.hasFieldErrors("startDate") should be (false)
		}
	}

	@Test
	def validateDates() { new Fixture {
		validator.validateDates(errors, new DateTime().toLocalDate, new DateTime().toLocalDate)
		errors.hasFieldErrors("startDate") should be (false)
		validator.validateDates(errors, new DateTime().toLocalDate, new DateTime().toLocalDate.plusDays(1))
		errors.hasFieldErrors("startDate") should be (false)
		validator.validateDates(errors, new DateTime().toLocalDate, new DateTime().toLocalDate.minusDays(1))
		errors.hasFieldErrors("startDate") should be (true)
	}}

	@Test
	def validateTypeMeeting() {
		new Fixture {
			validator.validateTypeMeeting(errors, mutable.Set(), mutable.Set(), 0, null)
			errors.hasFieldErrors("meetingRelationships") should be (true)
			errors.hasFieldErrors("meetingFormats") should be (true)
		}
		new Fixture {
			val department = new Department
			department.relationshipService = smartMock[RelationshipService]
			department.relationshipService.allStudentRelationshipTypes returns Seq()
			validator.validateTypeMeeting(errors, mutable.Set(StudentRelationshipType("tutor","tutor","tutor","tutee")), mutable.Set(), 0, department)
			errors.hasFieldErrors("meetingRelationships") should be (true)
		}
		new Fixture {
			val validRelationship = StudentRelationshipType("tutor","tutor","tutor","tutee")
			validRelationship.defaultDisplay = true
			val department = new Department
			department.relationshipService = smartMock[RelationshipService]
			department.relationshipService.allStudentRelationshipTypes returns Seq(validRelationship)
			validator.validateTypeMeeting(errors, mutable.Set(validRelationship), mutable.Set(), 0, department)
			errors.hasFieldErrors("meetingRelationships") should be (false)
		}
	}

	@Test
	def validateTypeSmallGroup() {
		new Fixture {
			validator.validateTypeSmallGroup(errors, JHashSet(), isAnySmallGroupEventModules = false, smallGroupEventQuantity = 0)
			errors.hasFieldErrors("smallGroupEventQuantity") should be (true)
			errors.hasFieldErrors("smallGroupEventModules") should be (true)
		}
		new Fixture {
			validator.validateTypeSmallGroup(errors, JHashSet(Fixtures.module("a100")), isAnySmallGroupEventModules = false, smallGroupEventQuantity = 1)
			errors.hasFieldErrors("smallGroupEventQuantity") should be (false)
			errors.hasFieldErrors("smallGroupEventModules") should be (false)
		}
	}

	@Test
	def validateTypeAssignmentSubmission() {
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				isSpecificAssignments = true,
				assignmentSubmissionQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionAssignments") should be (true)
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				isSpecificAssignments = true,
				assignmentSubmissionQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet(Fixtures.assignment("assignment"))
			)
			errors.hasFieldErrors("assignmentSubmissionAssignments") should be (false)
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				isSpecificAssignments = false,
				assignmentSubmissionQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionQuantity") should be (true)
			errors.hasFieldErrors("assignmentSubmissionModules") should be (true)
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				isSpecificAssignments = false,
				assignmentSubmissionQuantity = 1,
				assignmentSubmissionModules = JHashSet(Fixtures.module("a100")),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionQuantity") should be (false)
			errors.hasFieldErrors("assignmentSubmissionModules") should be (false)
		}
	}

	@Test
	def validateCanPointBeEditedByDate() {
		val startDate = DateTime.now.toLocalDate
		val autumnTerm = new TermImpl(null, null, null, TermType.autumn)
		val studentId = "1234"
		new Fixture {
			validator.termService.getTermFromDateIncludingVacations(startDate.toDateTimeAtStartOfDay) returns autumnTerm
			validator.attendanceMonitoringService.findReports(Seq(studentId), AcademicYear(2014), autumnTerm.getTermTypeAsString) returns Seq(new MonitoringPointReport)
			validator.validateCanPointBeEditedByDate(errors, startDate, Seq(studentId), AcademicYear(2014))
			errors.hasFieldErrors("startDate") should be (true)
		}
		new Fixture {
			validator.termService.getTermFromDateIncludingVacations(startDate.toDateTimeAtStartOfDay) returns autumnTerm
			validator.attendanceMonitoringService.findReports(Seq(studentId), AcademicYear(2014), autumnTerm.getTermTypeAsString) returns Seq()
			validator.validateCanPointBeEditedByDate(errors, startDate, Seq(studentId), AcademicYear(2014))
			errors.hasFieldErrors("startDate") should be (false)
		}
	}

	@Test
	def validateDuplicateForWeek() {
		val nonDupPoint = new AttendanceMonitoringPoint
		nonDupPoint.id = "1"
		nonDupPoint.name = "Name2"
		nonDupPoint.startWeek = 1
		nonDupPoint.endWeek = 1
		val dupPoint = new AttendanceMonitoringPoint
		dupPoint.id = "2"
		dupPoint.name = "Name"
		dupPoint.startWeek = 1
		dupPoint.endWeek = 1
		val schemeWithNonDupPoint = new AttendanceMonitoringScheme
		dupPoint.scheme = schemeWithNonDupPoint
		schemeWithNonDupPoint.points.add(nonDupPoint)
		val schemeWithDupPoint = new AttendanceMonitoringScheme
		schemeWithDupPoint.points.add(dupPoint)
		new Fixture {
			validator.validateDuplicateForWeek(errors, "Name", 1, 1, Seq(schemeWithNonDupPoint))
			errors.hasFieldErrors("name") should be (false)
			errors.hasFieldErrors("startWeek") should be (false)
		}
		new Fixture {
			validator.validateDuplicateForWeek(errors, "Name", 1, 1, Seq(schemeWithNonDupPoint, schemeWithDupPoint))
			errors.hasFieldErrors("name") should be (true)
			errors.hasFieldErrors("startWeek") should be (true)
		}
	}

	@Test
	def validateDuplicateForDate() {
		val baseDate = DateTime.now.toLocalDate
		val nonDupPoint = new AttendanceMonitoringPoint
		nonDupPoint.id = "1"
		nonDupPoint.name = "Name2"
		nonDupPoint.startDate = baseDate
		nonDupPoint.endDate = baseDate.plusDays(1)
		val dupPoint = new AttendanceMonitoringPoint
		dupPoint.id = "2"
		dupPoint.name = "Name"
		dupPoint.startDate = baseDate
		dupPoint.endDate = baseDate.plusDays(1)
		val schemeWithNonDupPoint = new AttendanceMonitoringScheme
		schemeWithNonDupPoint.points.add(nonDupPoint)
		val schemeWithDupPoint = new AttendanceMonitoringScheme
		schemeWithDupPoint.points.add(dupPoint)
		new Fixture {
			validator.validateDuplicateForDate(errors, "Name", baseDate, baseDate.plusDays(1), Seq(schemeWithNonDupPoint))
			errors.hasFieldErrors("name") should be (false)
			errors.hasFieldErrors("startDate") should be (false)
		}
		new Fixture {
			validator.validateDuplicateForDate(errors, "Name", baseDate, baseDate.plusDays(1), Seq(schemeWithNonDupPoint, schemeWithDupPoint))
			errors.hasFieldErrors("name") should be (true)
			errors.hasFieldErrors("startDate") should be (true)
		}
	}

}
