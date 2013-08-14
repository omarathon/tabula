package uk.ac.warwick.tabula.data.model.attendance

import org.joda.time.DateTime
import org.hibernate.annotations.Type

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull

import uk.ac.warwick.tabula.data.model.GeneratedId

@Entity
class MonitoringPoint extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "point_set_id")
	var pointSet: MonitoringPointSet = _
	
	@NotNull
	var name: String = _
	
	var defaultValue: Boolean = true
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var createdDate: DateTime = DateTime.now()
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var updatedDate: DateTime = _

	@NotNull
	var week: Int = _

}