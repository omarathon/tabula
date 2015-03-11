package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{AcademicYear, TestBase, Fixtures, Mockito}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringPointType, MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model._
import org.joda.time.{Interval, DateTimeConstants, DateMidnight, DateTime}

class MonitoringPointProfileTermAssessmentServiceTest extends TestBase with Mockito {

	val mockProfileService = mock[ProfileService]
	val mockMonitoringPointService = mock[MonitoringPointService]
	val mockModuleAndDepartmentService = mock[ModuleAndDepartmentService]
	val mockTermService = mock[TermService]
	val mockAssignmentService = mock[AssessmentService]

	trait ServiceTestSupport extends MonitoringPointServiceComponent
		with TermServiceComponent	with ProfileServiceComponent with AssessmentServiceComponent {

		val monitoringPointService = mockMonitoringPointService
		val assessmentService = mockAssignmentService
		val profileService = mockProfileService
		val termService = mockTermService

	}

	trait StudentFixture {
		val service = new AbstractMonitoringPointProfileTermAssignmentService with ServiceTestSupport

		val academicYear2012 = AcademicYear(2012)
		val academicYear2014 = AcademicYear(2014)

		val student = Fixtures.student("1234")

		val assignment = new Assignment
		assignment.openDate = new DateTime().minusDays(1)
		assignment.closeDate = new DateTime().plusMonths(1)
		assignment.openEnded = false

		val submission = new Submission
		submission.userId = student.userId
		submission.universityId = student.universityId
		submission.submittedDate = new DateTime()
		submission.assignment = assignment

		assignment.addSubmission(submission)

		mockProfileService.getMemberByUniversityId(student.universityId) returns Option(student)
		mockTermService.getAcademicWeekForAcademicYear(submission.assignment.closeDate, academicYear2014) returns 3

		val module1 = Fixtures.module("aa101")
		module1.id = "aa101"
		val module2 = Fixtures.module("aa202")
		module2.id = "aa202"
		mockModuleAndDepartmentService.getModuleById(module1.id) returns Option(module1)
		mockModuleAndDepartmentService.getModuleById(module2.id) returns Option(module2)

		mockAssignmentService.getAssignmentById(assignment.id) returns Option(assignment)

		val week5StartDate = new DateMidnight(academicYear2014.startYear, DateTimeConstants.NOVEMBER, 1)
		val week5EndDate = new DateMidnight(academicYear2014.startYear, DateTimeConstants.NOVEMBER, 8)
		val week15StartDate = new DateMidnight(academicYear2014.startYear, DateTimeConstants.DECEMBER, 1)
		val week15EndDate = new DateMidnight(academicYear2014.startYear, DateTimeConstants.DECEMBER, 8)

		val week5pair = (new Integer(5), new Interval(week5StartDate, week5EndDate))
		val week15pair = (new Integer(15), new Interval(week15StartDate, week15EndDate))
		val weeksForYear = Seq(week5pair, week15pair)
		mockTermService.getAcademicWeeksForYear(new DateMidnight(academicYear2014.startYear, DateTimeConstants.NOVEMBER, 1))	returns weeksForYear

	}

	trait Year2PointSetFixture extends StudentFixture {
		val year2PointSet = new MonitoringPointSet
		year2PointSet.academicYear = academicYear2014
		year2PointSet.year = 2

		service.monitoringPointService.getPointSetForStudent(student, academicYear2014) returns Option(year2PointSet)

	}

	trait ValidYear2PointFixture extends Year2PointSetFixture {
		val assignmentThisYearPoint = new MonitoringPoint
		assignmentThisYearPoint.pointSet = year2PointSet
		year2PointSet.points = JArrayList(assignmentThisYearPoint)
		assignmentThisYearPoint.validFromWeek = 1
		assignmentThisYearPoint.requiredFromWeek = 5
		assignmentThisYearPoint.pointType = MonitoringPointType.AssignmentSubmission
		assignmentThisYearPoint.assignmentSubmissionQuantity = 1
		assignmentThisYearPoint.assignmentSubmissionIsDisjunction = true
		assignmentThisYearPoint.assignmentSubmissionAssignments = Seq(new Assignment, assignment)
		assignmentThisYearPoint.assignmentSubmissionModules = Seq()

		assignmentThisYearPoint.assignmentService = mockAssignmentService
		assignmentThisYearPoint.moduleAndDepartmentService = mockModuleAndDepartmentService

		mockMonitoringPointService.getCheckpoint(student, assignmentThisYearPoint ) returns None
		mockMonitoringPointService.studentAlreadyReportedThisTerm(student, assignmentThisYearPoint) returns false

	}

	@Test
	def updatesCheckpointAnyAssignment() { new ValidYear2PointFixture {

		service.getCheckpointsForSubmission(submission).size should be (1)

		service.updateCheckpointsForSubmission(submission)
		verify(service.monitoringPointService, times(1)).saveOrUpdateCheckpointByUsercode(student, assignmentThisYearPoint, AttendanceState.Attended, student.userId, autocreated = true)
	}}

	@Test
	def updatesCheckpointSpecificModule() { new ValidYear2PointFixture {

		assignmentThisYearPoint.assignmentSubmissionIsSpecificAssignments = false
		assignmentThisYearPoint.assignmentSubmissionModules = Seq(module1)
		assignment.module = module1

		service.getCheckpointsForSubmission(submission).size should be (1)

		service.updateCheckpointsForSubmission(submission)
		verify(service.monitoringPointService, times(1)).saveOrUpdateCheckpointByUsercode(student, assignmentThisYearPoint, AttendanceState.Attended, student.userId, autocreated = true)
	}}

	@Test
	def lateSubmission() { new ValidYear2PointFixture {

		submission.submittedDate = assignment.closeDate.plusDays(1)
		service.getCheckpointsForSubmission(submission).size should be (0)

	}}

	@Test
	def noPointSet() { new ValidYear2PointFixture {

		service.monitoringPointService.getPointSetForStudent(student, academicYear2014) returns None
		service.getCheckpointsForSubmission(submission).size should be (0)

	}}

	@Test
	def wrongPointType() { new ValidYear2PointFixture {

		assignmentThisYearPoint.pointType = MonitoringPointType.Meeting
		service.getCheckpointsForSubmission(submission).size should be (0)

	}}

	@Test
	def wrongModule() { new ValidYear2PointFixture {

		assignmentThisYearPoint.assignmentSubmissionModules = Seq(module2)
		assignmentThisYearPoint.assignmentSubmissionIsSpecificAssignments = false
		service.getCheckpointsForSubmission(submission).size should be (0)

	}}

	@Test
	def checkpointAlreadyExists() { new ValidYear2PointFixture {

		mockMonitoringPointService.getCheckpoint(student, assignmentThisYearPoint ) returns Option(Fixtures.monitoringCheckpoint(assignmentThisYearPoint, student, AttendanceState.Attended))

		service.getCheckpointsForSubmission(submission).size should be (0)

	}}

	@Test
	def reportedToSITS() { new ValidYear2PointFixture {

		mockMonitoringPointService.studentAlreadyReportedThisTerm(student, assignmentThisYearPoint ) returns true
		service.getCheckpointsForSubmission(submission).size should be (0)

	}}

}
