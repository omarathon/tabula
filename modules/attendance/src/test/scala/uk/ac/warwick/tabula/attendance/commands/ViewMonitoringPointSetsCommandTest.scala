package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services.TermService
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSetTemplate, MonitoringPoint, AbstractMonitoringPointSet}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import org.junit.Ignore

class ViewMonitoringPointSetsCommandTest extends TestBase with Mockito {

	@Test
	def stateYear() {
		new StateFixture {
			state.academicYearToUse should be (AcademicYear(2009))
		}
	}

	@Ignore @Test
	def stateGrouping() {
		new StateFixture {
			val points = JArrayList(
				new MonitoringPoint,
				new MonitoringPoint
			)
			mockSet.points = points
			state.monitoringPointsByTerm
		}
	}

	trait StateFixture {
		val mockSet = new MonitoringPointSetTemplate

		val mockTermService = smartMock[TermService]
		val state = new ViewMonitoringPointSetState {
			def set: AbstractMonitoringPointSet = mockSet
			def termService: TermService = mockTermService
		}
		state.academicYear = AcademicYear(2009)
	}
}
