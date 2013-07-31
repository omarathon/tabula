package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.GeneratedId
import org.hibernate.annotations.Entity
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.Route
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class MonitoringPointSetYear extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "route_id")
	var route: Route = _
	
	@NotNull
	var year: Int = _
	
	var sentToAcademicOffice: Boolean = false

}