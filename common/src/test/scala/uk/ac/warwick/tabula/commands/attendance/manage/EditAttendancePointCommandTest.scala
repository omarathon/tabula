package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.{DateTime, DateTimeConstants, Interval, LocalDate}
import org.mockito.Matchers
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports.JHashSet
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.{Department, ScheduledNotification, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.util.termdates.AcademicYearPeriod.PeriodType

import scala.collection.mutable

class EditAttendancePointCommandTest extends TestBase with Mockito {

	trait Fixture {
		val commandState = new EditAttendancePointCommandState with SmallGroupServiceComponent with ModuleAndDepartmentServiceComponent {
			val templatePoint = null
			val department = null
			val academicYear = null
			val smallGroupService = null
			val moduleAndDepartmentService = null
		}
		val validator = new AttendanceMonitoringPointValidation with AttendanceMonitoringServiceComponent {
			val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		}
		val errors = new BindException(commandState, "command")
	}

	trait CommandFixture extends Fixture {

		val academicYear2015 = AcademicYear(2015)
		val department = new Department
		val student: StudentMember = Fixtures.student("1234")

		val scheme = new AttendanceMonitoringScheme
		scheme.academicYear = academicYear2015
		scheme.department = department
		scheme.pointStyle = AttendanceMonitoringPointStyle.Week
		scheme.members.addUserId(student.universityId)
		val templatePoint: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme, "name1", 0, 1)
		templatePoint.id = "123"
		val originalCreatedDate: DateTime = new DateTime().minusDays(2)
		templatePoint.createdDate = originalCreatedDate
		scheme.points.add(templatePoint)
		val scheme2 = new AttendanceMonitoringScheme
		scheme2.department = department
		scheme2.academicYear = academicYear2015
		scheme2.pointStyle = AttendanceMonitoringPointStyle.Week
		val secondPoint: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme2, templatePoint.name, templatePoint.startWeek, templatePoint.endWeek)
		secondPoint.id = "234"
		secondPoint.createdDate = originalCreatedDate
		scheme2.points.add(secondPoint)

		val command = new EditAttendancePointCommandInternal(null, null, templatePoint) with AttendanceMonitoringServiceComponent
			with EditAttendancePointCommandState with SmallGroupServiceComponent with ModuleAndDepartmentServiceComponent with ProfileServiceComponent {
			override def pointsToEdit = Seq(templatePoint, secondPoint)
			val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
			val smallGroupService = null
			val moduleAndDepartmentService = null
			val profileService: ProfileService = smartMock[ProfileService]
			profileService.getAllMembersWithUniversityIds(Seq(student.universityId)) returns Seq(student)
			startWeek = 5
			endWeek = 15
		}
		command.thisScheduledNotificationService = smartMock[ScheduledNotificationService]
		command.attendanceMonitoringService.listAllSchemes(department) returns Seq(scheme, scheme2)
	}

	@Test
	def testApply(): Unit = withFakeTime(new DateTime(2015, DateTimeConstants.NOVEMBER, 30, 12, 35, 0, 0)) { new CommandFixture {
		val points: Seq[AttendanceMonitoringPoint] = command.applyInternal()
		points.foreach(_.createdDate should be (originalCreatedDate))
		points.foreach(_.updatedDate.isAfter(originalCreatedDate) should be {true})
		points.find(_.id == templatePoint.id).get.scheme should be (scheme)
		points.find(_.id == secondPoint.id).get.scheme should be (scheme2)
		points.size should be (command.pointsToEdit.size)
		scheme.points.size should be (1)
		scheme2.points.size should be (1)
		verify(command.thisScheduledNotificationService, times(1)).removeInvalidNotifications(department)
		verify(command.thisScheduledNotificationService, atLeast(1)).push(Matchers.any[ScheduledNotification[Department]])
		verify(command.attendanceMonitoringService, times(1)).setCheckpointTotalsForUpdate(Seq(student), department, academicYear2015)
	}}

	@Test
	def validateName() { new Fixture {
		validator.validateName(errors, "Name")
		errors.hasFieldErrors("name") should be {false}
		validator.validateName(errors, "")
		errors.hasFieldErrors("name") should be {true}
	}}

	@Test
	def validateWeek() { new Fixture {
		validator.validateWeek(errors, 1, "startWeek")
		errors.hasFieldErrors("startWeek") should be {false}
		validator.validateWeek(errors, 53, "startWeek")
		errors.hasFieldErrors("startWeek") should be {true}
	}}

	@Test
	def validateWeeks() { new Fixture {
		validator.validateWeeks(errors, 2, 3)
		errors.hasFieldErrors("startWeek") should be {false}
		validator.validateWeeks(errors, 2, 1)
		errors.hasFieldErrors("startWeek") should be {true}
	}}

	@Test
	def validateDate() {
		new Fixture {
			validator.validateDate(errors, null, null, "startDate")
			errors.hasFieldErrors("startDate") should be {true}
		}
		new Fixture {
			val date: DateTime = new DateTime().withYear(2013)
			withFakeTime(date) {
				validator.validateDate(errors, date.toLocalDate, AcademicYear(2014), "startDate")
				errors.hasFieldErrors("startDate") should be(true)
			}
		}
		new Fixture {
			val date: DateTime = new DateTime().withYear(2016)
			withFakeTime(date) {
				validator.validateDate(errors, date.toLocalDate, AcademicYear(2014), "startDate")
				errors.hasFieldErrors("startDate") should be(true)
			}
		}
		new Fixture {
			val date: DateTime = new DateTime(2014, DateTimeConstants.NOVEMBER, 1, 9, 50, 22, 0) // Autumn term, 14/15
			withFakeTime(date) {
				validator.validateDate(errors, date.toLocalDate, AcademicYear(2014), "startDate")
				errors.hasFieldErrors("startDate") should be(false)
			}
		}
	}

	@Test
	def validateDates() { new Fixture {
		validator.validateDates(errors, new DateTime().toLocalDate, new DateTime().toLocalDate)
		errors.hasFieldErrors("startDate") should be {false}
		validator.validateDates(errors, new DateTime().toLocalDate, new DateTime().toLocalDate.plusDays(1))
		errors.hasFieldErrors("startDate") should be {false}
		validator.validateDates(errors, new DateTime().toLocalDate, new DateTime().toLocalDate.minusDays(1))
		errors.hasFieldErrors("startDate") should be {true}
	}}

	@Test
	def validateTypeForEndDate() { new Fixture {
		validator.validateTypeForEndDate(errors, AttendanceMonitoringPointType.Standard, new DateTime().toLocalDate.minusDays(1))
		errors.hasFieldErrors("pointType") should be {false}
		validator.validateTypeForEndDate(errors, AttendanceMonitoringPointType.Meeting, new DateTime().toLocalDate)
		errors.hasFieldErrors("pointType") should be {false}
		validator.validateTypeForEndDate(errors, AttendanceMonitoringPointType.Meeting, new DateTime().toLocalDate.minusDays(1))
		errors.hasFieldErrors("pointType") should be {true}
	}}

	@Test
	def validateTypeMeeting() {
		new Fixture {
			validator.validateTypeMeeting(errors, mutable.Set(), mutable.Set(), 0, null)
			errors.hasFieldErrors("meetingRelationships") should be {true}
			errors.hasFieldErrors("meetingFormats") should be {true}
		}
		new Fixture {
			val department = new Department
			department.relationshipService = smartMock[RelationshipService]
			department.relationshipService.allStudentRelationshipTypes returns Seq()
			validator.validateTypeMeeting(errors, mutable.Set(StudentRelationshipType("tutor","tutor","tutor","tutee")), mutable.Set(), 0, department)
			errors.hasFieldErrors("meetingRelationships") should be {true}
		}
		new Fixture {
			val validRelationship = StudentRelationshipType("tutor","tutor","tutor","tutee")
			validRelationship.defaultDisplay = true
			val department = new Department
			department.relationshipService = smartMock[RelationshipService]
			department.relationshipService.allStudentRelationshipTypes returns Seq(validRelationship)
			validator.validateTypeMeeting(errors, mutable.Set(validRelationship), mutable.Set(), 0, department)
			errors.hasFieldErrors("meetingRelationships") should be {false}
		}
	}

	@Test
	def validateTypeSmallGroup() {
		new Fixture {
			validator.validateTypeSmallGroup(errors, JHashSet(), isAnySmallGroupEventModules = false, smallGroupEventQuantity = 0)
			errors.hasFieldErrors("smallGroupEventQuantity") should be {true}
			errors.hasFieldErrors("smallGroupEventModules") should be {true}
		}
		new Fixture {
			validator.validateTypeSmallGroup(errors, JHashSet(Fixtures.module("a100")), isAnySmallGroupEventModules = false, smallGroupEventQuantity = 1)
			errors.hasFieldErrors("smallGroupEventQuantity") should be {false}
			errors.hasFieldErrors("smallGroupEventModules") should be {false}
		}
	}

	@Test
	def validateTypeAssignmentSubmission() {
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionAssignments") should be {true}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet(Fixtures.assignment("assignment"))
			)
			errors.hasFieldErrors("assignmentSubmissionAssignments") should be {false}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionTypeModulesQuantity") should be {true}
			errors.hasFieldErrors("assignmentSubmissionModules") should be {true}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 1,
				assignmentSubmissionModules = JHashSet(Fixtures.module("a100")),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionTypeModulesQuantity") should be {false}
			errors.hasFieldErrors("assignmentSubmissionModules") should be {false}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Any,
				assignmentSubmissionTypeAnyQuantity = 0,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionTypeModulesQuantity") should be {false}
			errors.hasFieldErrors("assignmentSubmissionTypeAnyQuantity") should be {true}
		}
		new Fixture {
			validator.validateTypeAssignmentSubmission(
				errors,
				assignmentSubmissionType = AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Any,
				assignmentSubmissionTypeAnyQuantity = 1,
				assignmentSubmissionTypeModulesQuantity = 0,
				assignmentSubmissionModules = JHashSet(),
				assignmentSubmissionAssignments = JHashSet()
			)
			errors.hasFieldErrors("assignmentSubmissionTypeModulesQuantity") should be {false}
			errors.hasFieldErrors("assignmentSubmissionTypeAnyQuantity") should be {false}
		}
	}

	@Test
	def validateCanPointBeEditedByDate() {
		val startDate = new DateTime(2014, DateTimeConstants.NOVEMBER, 1, 9, 50, 22, 0) // Autumn term, 14/15
		val studentId = "1234"
		new Fixture { withFakeTime(startDate) {
			validator.attendanceMonitoringService.findReports(Seq(studentId), AcademicYear(2014), PeriodType.autumnTerm.toString) returns Seq(new MonitoringPointReport)
			validator.validateCanPointBeEditedByDate(errors, startDate.toLocalDate, Seq(studentId), AcademicYear(2014))
			errors.hasFieldErrors("startDate") should be {true}
		}}
		new Fixture { withFakeTime(startDate) {
			validator.attendanceMonitoringService.findReports(Seq(studentId), AcademicYear(2014), PeriodType.autumnTerm.toString) returns Seq()
			validator.validateCanPointBeEditedByDate(errors, startDate.toLocalDate, Seq(studentId), AcademicYear(2014))
			errors.hasFieldErrors("startDate") should be {false}
		}}
	}

	@Test
	def validateDuplicateForWeekForEdit() {
		val scheme = new AttendanceMonitoringScheme
		val nonDupPoint = new AttendanceMonitoringPoint
		nonDupPoint.id = "1"
		nonDupPoint.name = "Name2"
		nonDupPoint.startWeek = 1
		nonDupPoint.endWeek = 1
		nonDupPoint.scheme = scheme
		scheme.points.add(nonDupPoint)
		val editingPoint = new AttendanceMonitoringPoint
		editingPoint.id = "3"
		editingPoint.name = "Name"
		editingPoint.startWeek = 1
		editingPoint.endWeek = 1
		scheme.points.add(editingPoint)
		editingPoint.scheme = scheme

		new Fixture {
			validator.validateDuplicateForWeekForEdit(errors, "Name", 1, 1, editingPoint)
			errors.hasFieldErrors("name") should be {false}
			errors.hasFieldErrors("startWeek") should be {false}
		}
		new Fixture {
			val dupPoint = new AttendanceMonitoringPoint
			dupPoint.id = "2"
			dupPoint.name = "Name"
			dupPoint.startWeek = 1
			dupPoint.endWeek = 1
			scheme.points.add(dupPoint)
			dupPoint.scheme = scheme
			validator.validateDuplicateForWeekForEdit(errors, "Name", 1, 1, editingPoint)
			errors.hasFieldErrors("name") should be {true}
			errors.hasFieldErrors("startWeek") should be {true}
		}
	}

	@Test
	def validateDuplicateForDateForEdit() {
		val scheme = new AttendanceMonitoringScheme
		val baseDate = DateTime.now.toLocalDate
		val nonDupPoint = new AttendanceMonitoringPoint
		nonDupPoint.id = "1"
		nonDupPoint.name = "Name2"
		nonDupPoint.startDate = baseDate
		nonDupPoint.endDate = baseDate.plusDays(1)
		nonDupPoint.scheme = scheme
		scheme.points.add(nonDupPoint)
		val editingPoint = new AttendanceMonitoringPoint
		editingPoint.id = "3"
		editingPoint.name = "Name"
		editingPoint.startDate = baseDate
		editingPoint.endDate = baseDate.plusDays(1)
		scheme.points.add(editingPoint)
		editingPoint.scheme = scheme

		new Fixture {
			validator.validateDuplicateForDateForEdit(errors, "Name", baseDate, baseDate.plusDays(1), editingPoint)
			errors.hasFieldErrors("name") should be {false}
			errors.hasFieldErrors("startDate") should be {false}
		}
		new Fixture {
			val dupPoint = new AttendanceMonitoringPoint
			dupPoint.id = "2"
			dupPoint.name = "Name"
			dupPoint.startDate = baseDate
			dupPoint.endDate = baseDate.plusDays(1)
			scheme.points.add(dupPoint)
			dupPoint.scheme = scheme
			validator.validateDuplicateForDateForEdit(errors, "Name", baseDate, baseDate.plusDays(1), editingPoint)
			errors.hasFieldErrors("name") should be {true}
			errors.hasFieldErrors("startDate") should be {true}
		}
	}

}
