package uk.ac.warwick.tabula.commands.attendance

import uk.ac.warwick.tabula.commands.attendance.old.ViewMonitoringPointSetState
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services.TermService
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import org.junit.Ignore
import uk.ac.warwick.tabula.data.model.Route

class ViewMonitoringPointSetsCommandTest extends TestBase with Mockito {

	@Test
	def stateYear() {
		new StateFixture {
			state.academicYearToUse should be (AcademicYear(2009))
		}
	}

	trait StateFixture {
		val mockSet = new MonitoringPointSet
		mockSet.route = new Route
		mockSet.academicYear = AcademicYear(2009)

		val mockTermService = smartMock[TermService]
		val state = new ViewMonitoringPointSetState {
			def set = mockSet
			def termService: TermService = mockTermService
		}
		state.academicYear = AcademicYear(2009)
	}
}
