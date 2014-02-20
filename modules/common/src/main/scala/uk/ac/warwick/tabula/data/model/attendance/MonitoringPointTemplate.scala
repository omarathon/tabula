package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._

@Entity
class MonitoringPointTemplate extends CommonMonitoringPointProperties with MonitoringPointSettings {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "point_set_id")
	var pointSet: MonitoringPointSetTemplate = _
}
