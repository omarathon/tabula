package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringCheckpointState

class MonitoringCheckpointStateConverter extends TwoWayConverter[String, MonitoringCheckpointState] {
	
	override def convertRight(value: String) = 
		if (value.hasText) MonitoringCheckpointState.fromCode(value)
		else null

	override def convertLeft(state: MonitoringCheckpointState) = Option(state).map { _.dbValue }.orNull

}