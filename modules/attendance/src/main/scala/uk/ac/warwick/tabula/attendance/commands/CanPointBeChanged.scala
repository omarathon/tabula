package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.services.MonitoringPointServiceComponent
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint

trait CanPointBeChanged extends MonitoringPointServiceComponent {

	// TAB-1079
	def canPointBeUpdated(point: MonitoringPoint) = {
		!point.sentToAcademicOffice && monitoringPointService.countCheckpointsForPoint(point) == 0
	}

	// TAB-1079
	def canPointBeRemoved(point: MonitoringPoint) = {
		!point.sentToAcademicOffice && monitoringPointService.countCheckpointsForPoint(point) == 0
	}

}
