package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.commands.{CommandInternal, _}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPointType._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState.{Attended, NotRecorded}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.attendancemonitoring._
import uk.ac.warwick.tabula.services.{SubmissionServiceComponent, _}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}


object RecheckAttendanceCommand {
  def apply(department: Department, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint, user: CurrentUser) =
    new RecheckAttendanceCommandInternal(department, academicYear, templatePoint, user)
      with ComposableCommand[Seq[AttendanceMonitoringCheckpoint]]
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringProfileServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringSubmissionServiceComponent
      with AutowiringMeetingRecordServiceComponent
      with AutowiringSmallGroupServiceComponent
      with RecheckAttendanceCommandDescription
      with EditAttendancePointPermissions
      with RecheckAttendanceCommandState
      with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent
      with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
      with AutowiringAttendanceMonitoringEventAttendanceServiceComponent
      with SetsFindPointsResultOnCommandState
      with AutowiringModuleAndDepartmentServiceComponent
}

class RecheckAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear, val templatePoint: AttendanceMonitoringPoint, val user: CurrentUser) extends CommandInternal[Seq[AttendanceMonitoringCheckpoint]] {
  self: RecheckAttendanceCommandState with AttendanceMonitoringServiceComponent =>

  override protected def applyInternal(): Seq[AttendanceMonitoringCheckpoint] = {
    proposedChangesToNonReportedPoints.filter(_.proposedState == NotRecorded).foreach { change =>
      attendanceMonitoringService.deleteCheckpointDangerously(change.checkpoint)
    }

    proposedChangesToNonReportedPoints.filterNot(_.proposedState == NotRecorded).map { change =>
      attendanceMonitoringService.setAttendance(change.student, Map(change.point -> change.proposedState), user.userId, autocreated = true)
    }.flatMap(_._1)
  }
}

case class AttendanceChange(
  student: StudentMember,
  point: AttendanceMonitoringPoint,
  checkpoint: AttendanceMonitoringCheckpoint,
  currentState: AttendanceState,
  proposedState: AttendanceState,
  alreadyReported: Boolean
)

trait RecheckAttendanceCommandState extends EditAttendancePointCommandState {
  self: AttendanceMonitoringServiceComponent with ProfileServiceComponent with SubmissionServiceComponent with MeetingRecordServiceComponent with SmallGroupServiceComponent with AttendanceMonitoringCourseworkSubmissionServiceComponent with AttendanceMonitoringMeetingRecordServiceComponent with AttendanceMonitoringEventAttendanceServiceComponent with ModuleAndDepartmentServiceComponent =>

  lazy val autoRecordedCheckpoints: Seq[AttendanceMonitoringCheckpoint] = {
    pointsToEdit.flatMap { point =>
      val students = profileService.getAllMembersWithUniversityIds(point.scheme.members.members.toSeq).collect {
        case student: StudentMember => Some(student)
      }.flatten

      val startDate = point.startDate.toDateTimeAtStartOfDay
      val endDate = point.endDate.plusDays(1).toDateTimeAtStartOfDay

      templatePoint.pointType match {
        case AssignmentSubmission =>
          val submissions = students.flatMap(student => submissionService.getSubmissionsBetweenDates(student.userId, startDate, endDate))

          submissions.flatMap(attendanceMonitoringCourseworkSubmissionService.getCheckpoints)
        case Meeting =>
          val meetingRecords = students.flatMap(student => meetingRecordService.listBetweenDates(student, startDate, endDate))

          meetingRecords.flatMap(attendanceMonitoringMeetingRecordService.getCheckpoints)
        case SmallGroup =>
          val eventAttendance = smallGroupService.findAttendanceForStudentsBetweenDates(students, academicYear, startDate.toLocalDateTime, endDate.toLocalDateTime)

          attendanceMonitoringEventAttendanceService.getCheckpoints(eventAttendance)
        case Standard =>
          Nil
      }
    }
  }

  lazy val proposedChanges: Seq[AttendanceChange] = {
    pointsToEdit.flatMap { point =>
      val students = profileService.getAllMembersWithUniversityIds(point.scheme.members.members.toSeq).collect {
        case student: StudentMember => student
      }

      val checkpoints = attendanceMonitoringService.getAllCheckpoints(point)

      val proposedCheckpoints = autoRecordedCheckpoints.filter(_.point == point)

      students.flatMap { student =>
        val existingCheckpoint = checkpoints.find(_.student == student)
        val proposedCheckpoint = proposedCheckpoints.find(_.student == student)

        if (existingCheckpoint.map(_.state) == proposedCheckpoint.map(_.state)) {
          None
        } else if (existingCheckpoint.nonEmpty || proposedCheckpoint.nonEmpty) {
          Some(AttendanceChange(
            student = student,
            point = point,
            checkpoint = existingCheckpoint.orElse(proposedCheckpoint).get,
            currentState = existingCheckpoint.map(_.state).getOrElse(NotRecorded),
            proposedState = proposedCheckpoint.map(_.state).getOrElse(NotRecorded),
            alreadyReported = attendanceMonitoringService.studentAlreadyReportedThisTerm(student, point)
          ))
        } else {
          None
        }
      }
        // don't change from missed to not recorded
        .filterNot(change => change.currentState != Attended && change.proposedState == NotRecorded)
        .sortBy(_.student.userId)
    }
  }

  lazy val proposedChangesToAlreadyReportedPoints: Seq[AttendanceChange] = proposedChanges.filter(_.alreadyReported)

  lazy val proposedChangesToNonReportedPoints: Seq[AttendanceChange] = proposedChanges.filterNot(_.alreadyReported)

  lazy val otherToAttended: Seq[AttendanceChange] = proposedChanges.filter(c => c.currentState != Attended && c.proposedState == Attended)

  lazy val attendedToOther: Seq[AttendanceChange] = proposedChanges.filter(c => c.currentState == Attended && c.proposedState != Attended)
}

trait RecheckAttendanceCommandDescription extends Describable[Seq[AttendanceMonitoringCheckpoint]] {
  self: RecheckAttendanceCommandState =>

  override lazy val eventName = "RecheckAttendance"

  override def describe(d: Description) {
    d.property("checkpoints", proposedChanges.map(change => change.student.universityId -> change.currentState.dbValue))
  }

  override def describeResult(d: Description, result: Seq[AttendanceMonitoringCheckpoint]): Unit = {
    d.property("checkpoints", result.map(checkpoint => checkpoint.student.universityId -> checkpoint.state.dbValue))
  }
}
