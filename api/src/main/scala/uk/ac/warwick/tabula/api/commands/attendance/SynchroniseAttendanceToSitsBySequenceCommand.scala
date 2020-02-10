package uk.ac.warwick.tabula.api.commands.attendance

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.api.commands.attendance.SynchroniseAttendanceToSitsBySequenceCommand.{RequiredPermission, Result}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringSynchroniseAttendanceToSitsServiceComponent, SynchroniseAttendanceToSitsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object SynchroniseAttendanceToSitsBySequenceCommand {
  case class Result(student: StudentMember, academicYear: AcademicYear, missedPoints: Int)
  type Command = Appliable[Seq[Result]]
    with SynchroniseAttendanceToSitsBySequenceState
    with SynchroniseAttendanceToSitsBySequenceRequest
    with SelfValidating

  val RequiredPermission: Permission = Permissions.MonitoringPoints.Report

  def apply(department: Department, currentUser: CurrentUser): Command =
    new SynchroniseAttendanceToSitsBySequenceCommandInternal(department, currentUser)
      with ComposableCommand[Seq[Result]]
      with SynchroniseAttendanceToSitsBySequenceRequest
      with AutowiringSynchroniseAttendanceToSitsServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringAttendanceMonitoringServiceComponent
      with SynchroniseAttendanceToSitsBySequencePermissions
      with SynchroniseAttendanceToSitsBySequenceValidation
      with SynchroniseAttendanceToSitsBySequenceDescription
}

abstract class SynchroniseAttendanceToSitsBySequenceCommandInternal(val department: Department, val currentUser: CurrentUser)
  extends CommandInternal[Seq[Result]] with SynchroniseAttendanceToSitsBySequenceState {
  self: SynchroniseAttendanceToSitsBySequenceRequest
    with SynchroniseAttendanceToSitsServiceComponent =>

  override def applyInternal(): Seq[Result] =
    missedPoints.map { case (student, missedCount) =>
      // This must succeed, else throw an exception
      require(synchroniseAttendanceToSitsService.synchroniseToSits(student, academicYear, missedCount, currentUser.apparentUser), s"Couldn't synchronise $student missed monitoring points with SITS")

      Result(student, academicYear, missedCount)
    }.toSeq
}

trait SynchroniseAttendanceToSitsBySequenceState {
  def department: Department
  def currentUser: CurrentUser
}

trait SynchroniseAttendanceToSitsBySequenceRequest {
  var academicYear: AcademicYear = _
  var missedPoints: Map[StudentMember, Int] = _
}

trait SynchroniseAttendanceToSitsBySequenceValidation extends SelfValidating {
  self: SynchroniseAttendanceToSitsBySequenceRequest
    with SynchroniseAttendanceToSitsBySequenceState
    with SecurityServiceComponent
    with AttendanceMonitoringServiceComponent =>

  override def validate(errors: Errors): Unit = {
    val allStudents = missedPoints.keySet.toSeq

    if (allStudents.isEmpty) {
      errors.rejectValue("missedPoints", "NotEmpty")
    }

    if (academicYear == null) {
      errors.rejectValue("academicYear", "NotEmpty")
    }

    if (!errors.hasErrors) {
      allStudents.foreach { student =>
        if (student.mostSignificantCourseDetails.isEmpty) {
          errors.rejectValue("missedPoints", "monitoringPointReport.student.noSCD", Array(student.universityId), "")
        } else if (!student.mostSignificantCourseDetails.get.freshStudentCourseYearDetails.exists(_.academicYear == academicYear)) {
          errors.rejectValue("missedPoints", "monitoringPointReport.student.noSCYD", Array(student.universityId, academicYear.toString), "")
        }

        if (!securityService.can(currentUser, Permissions.MonitoringPoints.Report, student)) {
          errors.rejectValue("missedPoints", "monitoringPointReport.student.noPermission", Array(student.universityId), "")
        }

        // Is the student a member of any Tabula monitoring schemes?
        val points = attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
        if (points.nonEmpty) {
          val schemes = points.groupBy(_.scheme).keys.toSeq.sortBy(s => (s.department.code, s.displayName))
          errors.rejectValue("missedPoints", "monitoringPointReport.onTabulaScheme", Array(student.universityId, schemes.map(s => s"${s.department.name}: ${s.displayName}").mkString(", ")), "")
        }

        // Does the student have any monitoring checkpoints already reported?
        val checkpoints = attendanceMonitoringService.getCheckpoints(points, student).values.toSeq
        val nonActiveCheckpoints = attendanceMonitoringService.getNonActiveCheckpoints(student, None, academicYear, checkpoints)

        val allCheckpoints = checkpoints ++ nonActiveCheckpoints
        val synchronisedCheckpoints = allCheckpoints.filter { cp =>
          Option(cp.lastSynchronisedToSits).nonEmpty &&
          cp.state == AttendanceState.MissedUnauthorised
        }

        if (synchronisedCheckpoints.nonEmpty) {
          errors.rejectValue(
            "missedPoints",
            "monitoringPointReport.reportedTabulaCheckpoints",
            Array(
              student.universityId,
              synchronisedCheckpoints.map(cp => s"${cp.point.scheme.department.name}: ${cp.point.scheme.displayName}, ${cp.point.name}, ${cp.state.description}").mkString(", ")
            ),
            ""
          )
        }
      }
    }

    if (missedPoints.exists { case (_, points) => points < 0 }) {
      errors.rejectValue("missedPoints", "monitoringPointReport.missedPointsLessThanZero")
    }
  }
}

trait SynchroniseAttendanceToSitsBySequencePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: SynchroniseAttendanceToSitsBySequenceState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(RequiredPermission, mandatory(department))
  }
}

trait SynchroniseAttendanceToSitsBySequenceDescription extends Describable[Seq[Result]] {
  self: SynchroniseAttendanceToSitsBySequenceRequest
    with SynchroniseAttendanceToSitsBySequenceState =>

  override lazy val eventName = "SynchroniseAttendanceToSitsBySequence"

  override def describe(d: Description): Unit =
    d.department(department)
     .property("academicYear", academicYear.toString)

  override def describeResult(d: Description, results: Seq[Result]): Unit =
    d.property("missedPoints", results.map { r => r.student.universityId -> r.missedPoints }.toMap)
}
