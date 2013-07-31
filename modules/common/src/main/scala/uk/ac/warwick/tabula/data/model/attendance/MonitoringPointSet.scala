package uk.ac.warwick.tabula.data.model.attendance

import org.hibernate.annotations.Entity
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.GeneratedId
import javax.persistence.OrderColumn

@Entity
class MonitoringPointSet extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "point_set_year_id")
	var pointSetYear: MonitoringPointSetYear = _
	
	@OneToMany(mappedBy = "pointSet")
	@OrderColumn(name = "position")
	var points: JList[MonitoringPoint] = JArrayList()
	
	@NotNull
	var name: String = _
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var createdDate: DateTime = _
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var updatedDate: DateTime = _

}