package uk.ac.warwick.tabula.commands.attendance

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.attendance.StudentOverwriteReportedCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringCheckpointTotal, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object StudentOverwriteReportedCommand {
  type Result = (AttendanceMonitoringCheckpoint, AttendanceMonitoringCheckpointTotal)
  type Command = Appliable[Result] with StudentOverwriteReportedState with SelfValidating with PopulateOnForm
  val RequiredPermission: Permission = Permissions.MonitoringPoints.OverwriteReported

  def apply(student: StudentMember, point: AttendanceMonitoringPoint, user: CurrentUser): Command =
    new StudentOverwriteReportedCommandInternal(student, point, user)
      with ComposableCommand[Result]
      with StudentOverwriteReportedRequest
      with PopulatesStudentOverwriteReportedRequest
      with StudentOverwriteReportedPermissions
      with StudentOverwriteReportedValidation
      with StudentOverwriteReportedDescription
      with AutowiringAttendanceMonitoringServiceComponent
}

abstract class StudentOverwriteReportedCommandInternal(val student: StudentMember, val point: AttendanceMonitoringPoint, val user: CurrentUser)
  extends CommandInternal[Result]
    with StudentOverwriteReportedState {
  self: StudentOverwriteReportedRequest
    with AttendanceMonitoringServiceComponent =>

  override def applyInternal(): (AttendanceMonitoringCheckpoint, AttendanceMonitoringCheckpointTotal) = transactional() {
    val updatedCheckpointOrNull: AttendanceMonitoringCheckpoint =
      if (state == null) {
        // Remove any existing checkpoint
        checkpoint.foreach(attendanceMonitoringService.deleteCheckpointDangerously)

        null
      } else {
        val cp: AttendanceMonitoringCheckpoint = checkpoint.getOrElse {
          val newCheckpoint = new AttendanceMonitoringCheckpoint
          newCheckpoint.student = student
          newCheckpoint.point = point
          newCheckpoint
        }

        cp.autoCreated = false
        cp.setStateDangerously(state)
        cp.updatedBy = user.apparentId
        cp.updatedDate = DateTime.now

        attendanceMonitoringService.saveOrUpdateDangerously(cp)

        cp
      }

    val total = attendanceMonitoringService.updateCheckpointTotal(student, point.scheme.department, point.scheme.academicYear)

    (updatedCheckpointOrNull, total)
  }

}

trait StudentOverwriteReportedPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentOverwriteReportedState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(RequiredPermission, mandatory(student))
    p.PermissionCheck(RequiredPermission, mandatory(point).scheme)
  }
}

trait StudentOverwriteReportedValidation extends SelfValidating {
  self: StudentOverwriteReportedRequest
    with StudentOverwriteReportedState
    with AttendanceMonitoringServiceComponent =>

  override def validate(errors: Errors): Unit =
    // check that a note exists for authorised absences
    if (state == AttendanceState.MissedAuthorised && attendanceMonitoringService.getAttendanceNote(student, point).isEmpty) {
      errors.rejectValue("state", "monitoringCheckpoint.missedAuthorised.noNote")
    }
}

trait StudentOverwriteReportedDescription extends Describable[Result] {
  self: StudentOverwriteReportedState =>

  override lazy val eventName = "StudentOverwriteReported"

  override def describe(d: Description): Unit =
    d.studentIds(student.universityId)
     .attendanceMonitoringPoints(Seq(point))
}

trait StudentOverwriteReportedState {
  self: AttendanceMonitoringServiceComponent =>

  def student: StudentMember
  def point: AttendanceMonitoringPoint
  def user: CurrentUser

  lazy val checkpoint: Option[AttendanceMonitoringCheckpoint] =
    attendanceMonitoringService.getCheckpoints(Seq(point), student).values.headOption
}

trait StudentOverwriteReportedRequest {
  var state: AttendanceState = _
}

trait PopulatesStudentOverwriteReportedRequest extends PopulateOnForm {
  self: StudentOverwriteReportedRequest
    with StudentOverwriteReportedState
    with AttendanceMonitoringServiceComponent =>

  override def populate(): Unit =
    checkpoint.foreach { checkpoint =>
      state = checkpoint.state
    }
}
