package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.{FeaturesComponent, TestBase, Mockito}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.services.{MonitoringPointService, TermService, MonitoringPointServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.scheduling.services.{ExportAttendanceToSitsService, ExportAttendanceToSitsServiceComponent}

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