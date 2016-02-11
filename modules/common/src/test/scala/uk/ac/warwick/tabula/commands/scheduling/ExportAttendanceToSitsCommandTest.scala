package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.services.scheduling.{ExportAttendanceToSitsServiceComponent, ExportAttendanceToSitsService}
import uk.ac.warwick.tabula.services.{MonitoringPointService, MonitoringPointServiceComponent, TermService, TermServiceComponent}
import uk.ac.warwick.tabula.{FeaturesComponent, Mockito, TestBase}

class ExportAttendanceToSitsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent with MonitoringPointServiceComponent with FeaturesComponent with ExportAttendanceToSitsServiceComponent {
		val termService = mock[TermService]
		val monitoringPointService = mock[MonitoringPointService]
		val features = emptyFeatures
		val exportAttendanceToSitsService = mock[ExportAttendanceToSitsService]
	}

	val cmd = new ExportAttendanceToSitsCommand with CommandTestSupport
	cmd.monitoringPointService.findUnreportedReports returns Seq()


	@Test def featureDisabled {
		cmd.features.attendanceMonitoringReport = false
		cmd.applyInternal() shouldBe Seq()
		verify(cmd.monitoringPointService, times(0)).findUnreportedReports
	}

	@Test def featureEnabled {
		cmd.features.attendanceMonitoringReport = true
		cmd.applyInternal() shouldBe Seq()
		verify(cmd.monitoringPointService, times(1)).findUnreportedReports
	}

}