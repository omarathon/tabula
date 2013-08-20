package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.GeneratedId
import javax.persistence.Entity
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.Route
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import org.joda.time.DateTime
import org.hibernate.annotations.Type

@Entity
class MonitoringPoint extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "point_set_id")
	var pointSet: MonitoringPointSet = _
	
	@NotNull
	var name: String = _
	
	var defaultValue: Boolean = true
	
	var createdDate: DateTime = _
	
	var updatedDate: DateTime = _
	
	var week: Int = _

}