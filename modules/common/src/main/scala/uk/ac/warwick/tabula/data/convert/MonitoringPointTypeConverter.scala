package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointType, MonitoringCheckpointState}

class MonitoringPointTypeConverter extends TwoWayConverter[String, MonitoringPointType] {
	
	override def convertRight(value: String) = 
		if (value.hasText) MonitoringPointType.fromCode(value)
		else null

	override def convertLeft(pointType: MonitoringPointType) = Option(pointType).map { _.dbValue }.orNull

}