package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._
import javax.validation.constraints.NotNull

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("template")
class MonitoringPointSetTemplate extends AbstractMonitoringPointSet {

	@NotNull
	var templateName: String = _

	@NotNull
	var position: Int = _
}