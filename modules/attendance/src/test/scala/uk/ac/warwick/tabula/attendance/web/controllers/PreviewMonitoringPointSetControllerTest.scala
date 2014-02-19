package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPointSetTemplate}
import uk.ac.warwick.tabula.commands.Appliable

class PreviewMonitoringPointSetControllerTest extends TestBase with Mockito {
	val controller = new PreviewMonitoringPointSetController
	val command = mock[Appliable[MonitoringPointSet]]
	val set = new MonitoringPointSet
	command.apply() returns (set)

	@Test
	def display() {
		controller.display(command) should be (Mav("manage/set/preview", "set" -> set))
	}
}

class PreviewMonitoringPointSetTemplateControllerTest extends TestBase with Mockito {
	val controller = new PreviewMonitoringPointSetTemplateController
	val command = mock[Appliable[MonitoringPointSetTemplate]]
	val set = new MonitoringPointSetTemplate
	command.apply() returns (set)

	@Test
	def display() {
		controller.display(command) should be (Mav("manage/set/preview", "set" -> set))
	}
}
