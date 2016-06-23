package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{ExportAttendanceToSitsService, ExportAttendanceToSitsServiceComponent}
import uk.ac.warwick.tabula.services.{TermService, TermServiceComponent}
import uk.ac.warwick.tabula.{FeaturesComponent, Mockito, TestBase}

class ExportAttendanceToSitsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent with AttendanceMonitoringServiceComponent with FeaturesComponent with ExportAttendanceToSitsServiceComponent {
		val termService = mock[TermService]
		val attendanceMonitoringService = mock[AttendanceMonitoringService]
		val features = emptyFeatures
		val exportAttendanceToSitsService = mock[ExportAttendanceToSitsService]
	}

	val cmd = new ExportAttendanceToSitsCommand with CommandTestSupport
	cmd.attendanceMonitoringService.listUnreportedReports returns Seq()


	@Test def featureDisabled(): Unit = {
		cmd.features.attendanceMonitoringReport = false
		cmd.applyInternal() shouldBe Seq()
		verify(cmd.attendanceMonitoringService, times(0)).listUnreportedReports
	}

	@Test def featureEnabled(): Unit = {
		cmd.features.attendanceMonitoringReport = true
		cmd.applyInternal() shouldBe Seq()
		verify(cmd.attendanceMonitoringService, times(1)).listUnreportedReports
	}

}