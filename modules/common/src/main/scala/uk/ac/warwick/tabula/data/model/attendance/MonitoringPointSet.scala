package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.GeneratedId
import javax.persistence.Entity
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.Route
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.JavaImports.JList
import javax.persistence.OneToMany
import javax.persistence.OrderColumn
import org.joda.time.DateTime
import org.hibernate.annotations.Type

@Entity
class MonitoringPointSet extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "route_id")
	var route: Route = _
	
	@OneToMany(mappedBy = "pointSet")
	@OrderColumn(name = "position")
	var points: JList[MonitoringPoint] = JArrayList()

	var year: Int = _
	
	var sentToAcademicOffice: Boolean = false
	
	var createdDate: DateTime = _
	
	var updatedDate: DateTime = _
	
	@NotNull
	var templateName: String = _

}