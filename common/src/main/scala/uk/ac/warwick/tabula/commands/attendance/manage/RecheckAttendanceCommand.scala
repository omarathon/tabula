package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{CommandInternal, _}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPointType._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState.{Attended, NotRecorded}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.attendancemonitoring._
import uk.ac.warwick.tabula.services.{SubmissionServiceComponent, _}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import scala.jdk.CollectionConverters._


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
      with RecheckAttendanceCommandRequest
      with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent
      with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
      with AutowiringAttendanceMonitoringEventAttendanceServiceComponent
      with SetsFindPointsResultOnCommandState
      with AutowiringModuleAndDepartmentServiceComponent
}

class RecheckAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear, val templatePoint: AttendanceMonitoringPoint, val user: CurrentUser) extends CommandInternal[Seq[AttendanceMonitoringCheckpoint]] with PopulateOnForm {
  self: RecheckAttendanceCommandState with RecheckAttendanceCommandRequest with AttendanceMonitoringServiceComponent =>

  override def populate(): Unit = {
    students = proposedChangesToNonReportedPoints.map(_.student.universityId).asJava
  }

  override protected def applyInternal(): Seq[AttendanceMonitoringCheckpoint] = {
    transactional() {
      val selectedChanges = proposedChangesToNonReportedPoints.filter(c => students.contains(c.student.universityId))

      selectedChanges.filter(_.proposedState == NotRecorded).foreach { change =>
        attendanceMonitoringService.deleteCheckpointDangerously(change.checkpoint)
      }

      selectedChanges.filterNot(_.proposedState == NotRecorded).flatMap { change =>
        val (checkpoints, _) = attendanceMonitoringService.setAttendance(change.student, Map(change.point -> change.proposedState), user.userId, autocreated = true)

        checkpoints.headOption.map { checkpoint =>
          change.appendAttendanceNote.map { attendanceNote =>
            attendanceMonitoringService.appendToAttendanceNote(checkpoint.student, checkpoint.point, attendanceNote, user.apparentUser)
          }
          checkpoint
        }
      }
    }
  }
}

case class AttendanceChange(
  student: StudentMember,
  point: AttendanceMonitoringPoint,
  checkpoint: AttendanceMonitoringCheckpoint,
  currentState: AttendanceState,
  proposedState: AttendanceState,
  alreadyReported: Boolean,
  appendAttendanceNote: Option[String]
)

trait RecheckAttendanceCommandState extends EditAttendancePointCommandState {
  self: AttendanceMonitoringServiceComponent with ProfileServiceComponent with SubmissionServiceComponent with MeetingRecordServiceComponent with SmallGroupServiceComponent with AttendanceMonitoringCourseworkSubmissionServiceComponent with AttendanceMonitoringMeetingRecordServiceComponent with AttendanceMonitoringEventAttendanceServiceComponent with ModuleAndDepartmentServiceComponent with RecheckAttendanceCommandRequest =>

  lazy val autoRecordedCheckpoints: Seq[(AttendanceMonitoringCheckpoint, Option[String])] = {
    pointsToEdit.flatMap { point =>
      val students = profileService.getAllMembersWithUniversityIds(point.scheme.members.members.toSeq).collect {
        case student: StudentMember => Some(student)
      }.flatten

      val startDate = point.startDate.toDateTimeAtStartOfDay
      val endDate = point.endDate.plusDays(1).toDateTimeAtStartOfDay

      templatePoint.pointType match {
        case AssignmentSubmission =>
          val submissions = students.flatMap(student => submissionService.getSubmissionsBetweenDates(student.userId, startDate, endDate))

          submissions.flatMap(attendanceMonitoringCourseworkSubmissionService.getCheckpoints(_, onlyRecordable = false)).map(_ -> None)
        case Meeting =>
          val meetingRecords = students.flatMap(student => meetingRecordService.listBetweenDates(student, startDate, endDate))

          meetingRecords.flatMap(attendanceMonitoringMeetingRecordService.getCheckpoints(_, onlyRecordable = false)).map(_ -> None)
        case SmallGroup =>
          val eventAttendance = smallGroupService.findAttendanceForStudentsBetweenDates(students, academicYear, startDate.toLocalDateTime, endDate.toLocalDateTime)

          attendanceMonitoringEventAttendanceService.getCheckpoints(eventAttendance, onlyRecordable = false).map(_ -> None) ++
            attendanceMonitoringEventAttendanceService.getMissedCheckpoints(eventAttendance, onlyRecordable = false).map {
              case (checkpoint, eventNotes) => checkpoint -> attendanceMonitoringEventAttendanceService.summariseSmallGroupEventAttendanceNotes(eventNotes)
            }
        case Standard =>
          Nil
      }
    }
  }

  lazy val proposedChanges: Seq[AttendanceChange] = {
    if (templatePoint.pointType == Standard) {
      Nil
    } else {
      pointsToEdit.flatMap { point =>
        val students = profileService.getAllMembersWithUniversityIds(point.scheme.members.members.toSeq).collect {
          case student: StudentMember => student
        }

        val checkpoints = attendanceMonitoringService.getAllCheckpoints(point)

        val proposedCheckpoints = autoRecordedCheckpoints.filter(_._1.point == point)

        students.flatMap { student =>
          val existingCheckpoint = checkpoints.find(_.student == student)
          val proposedCheckpoint = proposedCheckpoints.find(_._1.student == student).map(_._1)
          val proposedNotes = proposedCheckpoints.find(_._1.student == student).flatMap(_._2)

          if (existingCheckpoint.map(_.state) == proposedCheckpoint.map(_.state)) {
            None
          } else if (existingCheckpoint.nonEmpty || proposedCheckpoint.nonEmpty) {
            Some(AttendanceChange(
              student = student,
              point = point,
              checkpoint = existingCheckpoint.orElse(proposedCheckpoint).get,
              currentState = existingCheckpoint.map(_.state).getOrElse(NotRecorded),
              proposedState = proposedCheckpoint.map(_.state).getOrElse(NotRecorded),
              alreadyReported = attendanceMonitoringService.studentAlreadyReportedThisTerm(student, point),
              appendAttendanceNote = proposedNotes
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
  }

  lazy val proposedChangesToAlreadyReportedPoints: Seq[AttendanceChange] = proposedChanges.filter(_.alreadyReported)

  lazy val proposedChangesToNonReportedPoints: Seq[AttendanceChange] = proposedChanges.filterNot(_.alreadyReported)

  lazy val otherToAttended: Seq[AttendanceChange] = proposedChanges.filter(c => c.currentState != Attended && c.proposedState == Attended)

  lazy val attendedToOther: Seq[AttendanceChange] = proposedChanges.filter(c => c.currentState == Attended && c.proposedState != Attended)
}

trait RecheckAttendanceCommandRequest {
  var students: JList[String] = _
}

trait RecheckAttendanceCommandDescription extends Describable[Seq[AttendanceMonitoringCheckpoint]] {
  self: RecheckAttendanceCommandState =>

  override lazy val eventName = "RecheckAttendance"

  override def describe(d: Description): Unit =
    d.attendanceMonitoringCheckpoints(proposedChanges.groupBy(_.student).view.mapValues { changes =>
      changes.map { change =>
        change.point -> change.currentState
      }.toMap
    }.toMap, verbose = true)

  override def describeResult(d: Description, result: Seq[AttendanceMonitoringCheckpoint]): Unit =
    d.attendanceMonitoringCheckpoints(result.groupBy(_.student).view.mapValues { checkpoints =>
      checkpoints.map { checkpoint =>
        checkpoint.point -> checkpoint.state
      }.toMap
    }.toMap, verbose = true)
}
