package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.TestBase

class AttendanceMonitoringCheckpointTest extends TestBase {

  @Test def transitionNeedsSynchronisingToSits(): Unit = {
    AttendanceMonitoringCheckpoint.transitionNeedsSynchronisingToSits(null, AttendanceState.Attended) should be (false)
    AttendanceMonitoringCheckpoint.transitionNeedsSynchronisingToSits(null, null) should be (false)
    AttendanceMonitoringCheckpoint.transitionNeedsSynchronisingToSits(null, AttendanceState.MissedUnauthorised) should be (true)
    AttendanceMonitoringCheckpoint.transitionNeedsSynchronisingToSits(AttendanceState.Attended, AttendanceState.MissedUnauthorised) should be (true)
    AttendanceMonitoringCheckpoint.transitionNeedsSynchronisingToSits(AttendanceState.MissedUnauthorised, AttendanceState.Attended) should be (true)
  }

}
