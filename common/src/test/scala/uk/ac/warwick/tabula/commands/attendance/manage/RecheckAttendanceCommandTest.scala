package uk.ac.warwick.tabula.commands.attendance.manage

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.attendance.GroupedPoint
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState.{Attended, MissedUnauthorised, NotRecorded}
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventAttendance
import uk.ac.warwick.tabula.data.model.{Department, MeetingRecord, StudentMember, Submission}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class RecheckAttendanceCommandTest extends TestBase with Mockito {

  trait Fixture {
    val department = new Department
    val academicYear = AcademicYear(2018)
    val scheme = new AttendanceMonitoringScheme
    scheme.pointStyle = AttendanceMonitoringPointStyle.Week

    val templatePoint = new AttendanceMonitoringPoint
    templatePoint.scheme = scheme
    templatePoint.startDate = new LocalDate(2019, 1, 1)
    templatePoint.endDate = new LocalDate(2019, 1, 10)

    val command = new RecheckAttendanceCommandInternal(department, academicYear, templatePoint, currentUser)
      with ComposableCommand[Seq[AttendanceMonitoringCheckpoint]]
      with RecheckAttendanceCommandState
      with AttendanceMonitoringServiceComponent
      with ProfileServiceComponent
      with SecurityServiceComponent
      with SubmissionServiceComponent
      with MeetingRecordServiceComponent
      with SmallGroupServiceComponent
      with RecheckAttendanceCommandDescription
      with EditAttendancePointPermissions
      with AttendanceMonitoringCourseworkSubmissionServiceComponent
      with AttendanceMonitoringMeetingRecordServiceComponent
      with AttendanceMonitoringEventAttendanceServiceComponent
      with SetsFindPointsResultOnCommandState
      with ModuleAndDepartmentServiceComponent {
      val securityService: SecurityService = smartMock[SecurityService]
      val profileService: ProfileService = smartMock[ProfileService]
      val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
      val submissionService: SubmissionService = smartMock[SubmissionService]
      val meetingRecordService: MeetingRecordService = smartMock[MeetingRecordService]
      val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
      val attendanceMonitoringCourseworkSubmissionService: AttendanceMonitoringCourseworkSubmissionService = smartMock[AttendanceMonitoringCourseworkSubmissionService]
      val attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
      val attendanceMonitoringEventAttendanceService: AttendanceMonitoringEventAttendanceService = smartMock[AttendanceMonitoringEventAttendanceService]
      val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
      override val user: CurrentUser = new CurrentUser(new User("real"), new User("apparent"))
    }

    def buildCheckpoint(state: AttendanceState): AttendanceMonitoringCheckpoint = {
      val checkpoint = new AttendanceMonitoringCheckpoint
      checkpoint.point = templatePoint
      checkpoint.student = student
      checkpoint.attendanceMonitoringService = command.attendanceMonitoringService
      checkpoint.state = state
      checkpoint
    }

    val student = new StudentMember()
    student.universityId = "1234567"
    student.userId = "custrd"

    scheme.members.add(student.asSsoUser)

    command.setFindPointsResult(FindPointsResult(termGroupedPoints = Map("Autumn" -> Seq(GroupedPoint(templatePoint, schemes = Seq(scheme), points = Seq(templatePoint)))), monthGroupedPoints = Map.empty, courseworkAssignmentPoints = Nil))

    command.profileService.getAllMembersWithUniversityIds(Seq("1234567")) returns Seq(student)
  }

  trait CourseworkSubmissionFixture extends Fixture {
    templatePoint.pointType = AttendanceMonitoringPointType.AssignmentSubmission

    val submission = new Submission

    command.submissionService.getSubmissionsBetweenDates("custrd", new LocalDate(2019, 1, 1).toDateTimeAtStartOfDay, new LocalDate(2019, 1, 11).toDateTimeAtStartOfDay) returns Seq(submission)

    val proposedCheckpoint: AttendanceMonitoringCheckpoint = buildCheckpoint(state = Attended)
    command.attendanceMonitoringCourseworkSubmissionService.getCheckpoints(submission) returns Seq(proposedCheckpoint)
  }

  @Test def testDoesNothingForStandardPoint(): Unit = new Fixture {
    templatePoint.pointType = AttendanceMonitoringPointType.Standard

    command.autoRecordedCheckpoints shouldBe empty
    command.proposedChanges shouldBe empty
  }

  @Test def testApplyDoesNothingForReportedStudents(): Unit = new CourseworkSubmissionFixture {
    // No existing checkpoint
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Nil

    // Setting attendance returns this checkpoint
    val checkpoint = buildCheckpoint(state = Attended)

    // Student already reported
    command.attendanceMonitoringService.studentAlreadyReportedThisTerm(student, templatePoint) returns true

    val checkpoints = command.apply()

    // The checkpoint is set and returned
    checkpoints shouldBe empty
  }

  @Test def testApplySetsAttendance(): Unit = new CourseworkSubmissionFixture {
    // No existing checkpoint
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Nil

    // Setting attendance returns this checkpoint
    val checkpoint = buildCheckpoint(state = Attended)
    command.attendanceMonitoringService.setAttendance(student, Map(templatePoint -> Attended), "apparent", autocreated = true) returns ((Seq(checkpoint), Seq.empty[AttendanceMonitoringCheckpointTotal]))

    val checkpoints = command.apply()

    // The checkpoint is set and returned
    checkpoints should contain only checkpoint
  }

  @Test def testApplyDeletesCheckpointsDangerously(): Unit = new Fixture {
    templatePoint.pointType = AttendanceMonitoringPointType.AssignmentSubmission

    // The student has not submitted
    command.submissionService.getSubmissionsBetweenDates("custrd", new LocalDate(2019, 1, 1).toDateTimeAtStartOfDay, new LocalDate(2019, 1, 11).toDateTimeAtStartOfDay) returns Nil

    // Checkpoint recorded as Attended
    val existingCheckpoint = buildCheckpoint(state = Attended)
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Seq(existingCheckpoint)

    val checkpoints = command.apply()

    verify(command.attendanceMonitoringService).deleteCheckpointDangerously(existingCheckpoint)

    checkpoints shouldBe empty
  }

  @Test def testDoesNotDeleteMissedPoints(): Unit = new Fixture {
    templatePoint.pointType = AttendanceMonitoringPointType.AssignmentSubmission

    // The student has not submitted
    command.submissionService.getSubmissionsBetweenDates("custrd", new LocalDate(2019, 1, 1).toDateTimeAtStartOfDay, new LocalDate(2019, 1, 11).toDateTimeAtStartOfDay) returns Nil

    // Checkpoint recorded as MissedUnauthorised
    val existingCheckpoint = buildCheckpoint(state = MissedUnauthorised)
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Seq(existingCheckpoint)

    val checkpoints = command.apply()

    // Should not delete the point, even though auto-record would set the point to NotRecorded
    verify(command.attendanceMonitoringService, never()).deleteCheckpointDangerously(existingCheckpoint)

    checkpoints shouldBe empty
  }

  @Test def testCourseworkProposedChanges(): Unit = new CourseworkSubmissionFixture {
    // The student has submitted coursework that triggers auto-recording for this point

    // No existing checkpoint
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Nil

    // Propose recording the point as Attended
    command.proposedChanges should contain only AttendanceChange(
      student = student,
      point = templatePoint,
      checkpoint = proposedCheckpoint,
      currentState = NotRecorded,
      proposedState = Attended,
      alreadyReported = false
    )
  }

  @Test def testAttendedCourseworkPointNoChanges(): Unit = new Fixture with CourseworkSubmissionFixture {
    // The student has submitted coursework that triggers auto-recording for this point

    // Checkpoint recorded as Attended
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Seq(buildCheckpoint(state = Attended))

    // No proposed changes
    command.proposedChanges shouldBe empty
  }

  @Test def testUnrecordedCourseworkPointNoChanges(): Unit = new Fixture {
    templatePoint.pointType = AttendanceMonitoringPointType.AssignmentSubmission

    // The student has not submitted
    command.submissionService.getSubmissionsBetweenDates("custrd", new LocalDate(2019, 1, 1).toDateTimeAtStartOfDay, new LocalDate(2019, 1, 11).toDateTimeAtStartOfDay) returns Nil

    // No existing checkpoint
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Nil

    // No proposed changes
    command.proposedChanges shouldBe empty
  }

  trait MeetingRecordFixture extends Fixture {
    templatePoint.pointType = AttendanceMonitoringPointType.Meeting

    val meetingRecord: MeetingRecord = new MeetingRecord

    command.meetingRecordService.listBetweenDates(student, new LocalDate(2019, 1, 1).toDateTimeAtStartOfDay, new LocalDate(2019, 1, 11).toDateTimeAtStartOfDay) returns Seq(meetingRecord)

    val proposedCheckpoint = buildCheckpoint(state = Attended)
    command.attendanceMonitoringMeetingRecordService.getCheckpoints(meetingRecord) returns Seq(proposedCheckpoint)
  }

  @Test def testMeetingProposedChanges(): Unit = new MeetingRecordFixture {
    // The student has a meeting record that triggers auto-recording for this point

    // No existing checkpoint
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Nil

    // Propose recording the point as Attended
    command.proposedChanges should contain only AttendanceChange(
      student = student,
      point = templatePoint,
      checkpoint = proposedCheckpoint,
      currentState = NotRecorded,
      proposedState = Attended,
      alreadyReported = false
    )
  }

  @Test def testAttendedMeetingProposedChanges(): Unit = new MeetingRecordFixture {
    // The student has a meeting record that triggers auto-recording for this point

    // Checkpoint recorded as Attended
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Seq(buildCheckpoint(state = Attended))

    // No proposed changes
    command.proposedChanges shouldBe empty
  }

  @Test def testUnrecordedMeetingNoChanges(): Unit = new Fixture {
    templatePoint.pointType = AttendanceMonitoringPointType.Meeting

    // The student has no meeting record
    command.meetingRecordService.listBetweenDates(student, new LocalDate(2019, 1, 1).toDateTimeAtStartOfDay, new LocalDate(2019, 1, 11).toDateTimeAtStartOfDay) returns Nil

    // No existing checkpoint
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Nil

    // No proposed changes
    command.proposedChanges shouldBe empty
  }

  trait SmallGroupEventFixture extends Fixture {
    templatePoint.pointType = AttendanceMonitoringPointType.SmallGroup

    val smallGroupEventAttendance: SmallGroupEventAttendance = new SmallGroupEventAttendance

    command.smallGroupService.findAttendanceForStudentsBetweenDates(Seq(student), academicYear, new LocalDate(2019, 1, 1).toDateTimeAtStartOfDay.toLocalDateTime, new LocalDate(2019, 1, 11).toDateTimeAtStartOfDay.toLocalDateTime) returns Seq(smallGroupEventAttendance)

    val proposedCheckpoint = buildCheckpoint(state = Attended)
    command.attendanceMonitoringEventAttendanceService.getCheckpoints(Seq(smallGroupEventAttendance)) returns Seq(proposedCheckpoint)
    command.attendanceMonitoringEventAttendanceService.getMissedCheckpoints(Seq(smallGroupEventAttendance)) returns Nil
  }

  @Test def testSmallGroupEventAttendanceProposedChanges(): Unit = new SmallGroupEventFixture {
    // The student has small group attendance that triggers auto-recording for this point

    // No existing checkpoint
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Nil

    // Propose recording the point as Attended
    command.proposedChanges should contain only AttendanceChange(
      student = student,
      point = templatePoint,
      checkpoint = proposedCheckpoint,
      currentState = NotRecorded,
      proposedState = Attended,
      alreadyReported = false
    )
  }

  @Test def testAttendedSmallGroupEventAttendanceNoChanges(): Unit = new SmallGroupEventFixture {
    // The student has small group attendance that triggers auto-recording for this point

    // Checkpoint recorded as attended
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Seq(buildCheckpoint(state = Attended))

    // No proposed changes
    command.proposedChanges shouldBe empty
  }

  @Test def testUnrecordedSmallGroupEventAttendanceNoChanges(): Unit = new SmallGroupEventFixture {
    templatePoint.pointType = AttendanceMonitoringPointType.SmallGroup

    // The student has no small group attendance
    command.smallGroupService.findAttendanceForStudentsBetweenDates(Seq(student), academicYear, new LocalDate(2019, 1, 1).toDateTimeAtStartOfDay.toLocalDateTime, new LocalDate(2019, 1, 11).toDateTimeAtStartOfDay.toLocalDateTime) returns Nil
    command.attendanceMonitoringEventAttendanceService.getMissedCheckpoints(Nil) returns Nil

    // No attendance means no checkpoints
    command.attendanceMonitoringEventAttendanceService.getCheckpoints(Nil) returns Nil

    // No existing checkpoint
    command.attendanceMonitoringService.getAllCheckpoints(templatePoint) returns Nil

    // No proposed changes
    command.proposedChanges shouldBe empty
  }
}
