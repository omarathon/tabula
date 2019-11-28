package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand, Describable, Description}
import SynchroniseAttendanceToSitsCommand._
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringCheckpoint
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.{AutowiringSynchroniseAttendanceToSitsServiceComponent, SynchroniseAttendanceToSitsServiceComponent}

object SynchroniseAttendanceToSitsCommand {
  val BatchSize = 100

  type Result = Seq[AttendanceMonitoringCheckpoint]
  type Command = Appliable[Result]

  def apply(): Command =
    new SynchroniseAttendanceToSitsCommandInternal
      with ComposableCommand[Result]
      with SynchroniseAttendanceToSitsPermissions
      with SynchroniseAttendanceToSitsDescription
      with AutowiringFeaturesComponent
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringSynchroniseAttendanceToSitsServiceComponent
}

abstract class SynchroniseAttendanceToSitsCommandInternal extends CommandInternal[Result] with Logging {
  self: FeaturesComponent
    with AttendanceMonitoringServiceComponent
    with SynchroniseAttendanceToSitsServiceComponent =>

  override def applyInternal(): Seq[AttendanceMonitoringCheckpoint] = transactional() {
    if (features.attendanceMonitoringRealTimeReport) {
      val unsynchronisedCheckpoints = attendanceMonitoringService.listUnsynchronisedCheckpoints(BatchSize)
      unsynchronisedCheckpoints.flatMap { checkpoint =>
        if (synchroniseAttendanceToSitsService.synchroniseToSits(checkpoint)) {
          attendanceMonitoringService.markCheckpointAsSynchronised(checkpoint)
          logger.debug(s"Synchronised ${checkpoint.id} for ${checkpoint.student.universityId} (${checkpoint.state})")
          Some(checkpoint)
        } else {
          logger.error(s"Could not synchronise ${checkpoint.id} with SITS for ${checkpoint.student.universityId}")
          None
        }
      }
    } else Seq.empty
  }
}

trait SynchroniseAttendanceToSitsPermissions extends RequiresPermissionsChecking {
  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(Permissions.MonitoringPoints.Export)
}

trait SynchroniseAttendanceToSitsDescription extends Describable[Result] {
  override lazy val eventName: String = "SynchroniseAttendanceToSits"

  override def describe(d: Description): Unit = {}

  override def describeResult(d: Description, checkpoints: Seq[AttendanceMonitoringCheckpoint]): Unit =
    d.attendanceMonitoringCheckpoints(
      checkpoints.groupBy(_.student).view
        .mapValues { checkpoints =>
          checkpoints.map { checkpoint =>
            checkpoint.point -> checkpoint.state
          }.toMap
        }.toMap,
      verbose = true
    )
}
