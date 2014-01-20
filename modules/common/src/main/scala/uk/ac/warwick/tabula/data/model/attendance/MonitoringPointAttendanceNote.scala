package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._

import uk.ac.warwick.tabula.data.model.AttendanceNote

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("monitoringPoint")
class MonitoringPointAttendanceNote extends AttendanceNote {

	@ManyToOne
	@JoinColumn(name = "parent_id")
	var point: MonitoringPoint = _

}