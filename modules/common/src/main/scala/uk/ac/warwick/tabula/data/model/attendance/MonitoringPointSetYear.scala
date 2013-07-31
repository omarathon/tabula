package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.GeneratedId
import org.hibernate.annotations.Entity
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.Route
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.JavaImports.JList
import javax.persistence.OneToMany
import javax.persistence.OrderColumn

@Entity
class MonitoringPointSetYear extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "route_id")
	var route: Route = _
	
	@OneToMany(mappedBy = "pointSetYear")
	@OrderColumn(name = "position")
	var pointSets: JList[MonitoringPointSet] = JArrayList()
	
	@NotNull
	var year: Int = _
	
	var sentToAcademicOffice: Boolean = false

}