package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.GeneratedId
import org.hibernate.annotations.Entity
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
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var createdDate: DateTime = _
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var updatedDate: DateTime = _

}