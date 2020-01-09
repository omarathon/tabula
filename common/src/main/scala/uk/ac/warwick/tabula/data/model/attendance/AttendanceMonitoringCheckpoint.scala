package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringCheckpoint._
import uk.ac.warwick.tabula.data.model.{GeneratedId, StudentMember}
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService

object AttendanceMonitoringCheckpoint {
  def transitionNeedsSynchronisingToSits(transition: (AttendanceState, AttendanceState)): Boolean =
    transitionNeedsSynchronisingToSits(transition._1, transition._2)

  def transitionNeedsSynchronisingToSits(oldState: AttendanceState, newState: AttendanceState): Boolean =
    oldState -> newState match {
      // no-op
      case (s1, s2) if s1 == s2 => false

      // Anything -> MissedUnauthorised
      case (_, AttendanceState.MissedUnauthorised) => true

      // MissedUnauthorised -> Anything
      case (AttendanceState.MissedUnauthorised, _) => true

      case _ => false
    }
}

@Entity
@Proxy
class AttendanceMonitoringCheckpoint extends GeneratedId {

  @transient var attendanceMonitoringService: AttendanceMonitoringService = Wire[AttendanceMonitoringService]

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "point_id")
  @ForeignKey(name = "none")
  var point: AttendanceMonitoringPoint = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id")
  @ForeignKey(name = "none")
  var student: StudentMember = _

  @NotNull
  @Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.AttendanceStateUserType")
  @Column(name = "state")
  private var _state: AttendanceState = _

  def state: AttendanceState = _state

  def state_=(state: AttendanceState): Unit = {
    if (attendanceMonitoringService.studentAlreadyReportedThisTerm(student, point)) {
      throw new IllegalArgumentException
    }

    if (transitionNeedsSynchronisingToSits(_state -> state)) {
      needsSynchronisingToSits = true
    }

    _state = state
  }

  def setStateDangerously(state: AttendanceState): Unit = {
    if (transitionNeedsSynchronisingToSits(_state -> state)) {
      needsSynchronisingToSits = true
    }

    _state = state
  }

  @NotNull
  @Column(name = "updated_date")
  var updatedDate: DateTime = _

  @NotNull
  @Column(name = "updated_by")
  var updatedBy: String = _

  @NotNull
  var autoCreated: Boolean = false

  @NotNull
  var needsSynchronisingToSits: Boolean = false

  // null for any checkpoint that's never been synchronised
  var lastSynchronisedToSits: DateTime = _

  @transient
  var activePoint = true

}
