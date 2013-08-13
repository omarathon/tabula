package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.GeneratedId
import javax.persistence._
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.JavaImports.JList
import org.joda.time.DateTime
import org.hibernate.annotations.Type

@Entity
class MonitoringPointSet extends GeneratedId {
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "route_id")
	var route: Route = _
	
	@OneToMany(mappedBy = "pointSet", cascade=Array(CascadeType.PERSIST), orphanRemoval = true)
	@OrderBy("week")
	var points: JList[MonitoringPoint] = JArrayList()

	// Can be null, which indicates it applies to all years on this course.
	var year: JInteger = _
	
	var sentToAcademicOffice: Boolean = false
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var createdDate: DateTime = _
	
	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	var updatedDate: DateTime = _
	
	@NotNull
	var templateName: String = _

	def add(point: MonitoringPoint) {
		points.add(point)
		point.pointSet = this
	}

	def remove(point: MonitoringPoint) {
		points.remove(point)
		point.pointSet = null
	}
}