package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.GeneratedId
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.JavaImports.JList
import org.joda.time.DateTime
import org.hibernate.annotations.BatchSize

@Entity
@Table(name = "monitoringpointset")
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
abstract class AbstractMonitoringPointSet extends GeneratedId {
	
	@OneToMany(mappedBy = "pointSet", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@OrderBy("week")
	@BatchSize(size=100)
	var points: JList[MonitoringPoint] = JArrayList()
	
	var createdDate: DateTime = _
	
	var updatedDate: DateTime = _

	def add(point: MonitoringPoint) {
		points.add(point)
		point.pointSet = this
	}

	def remove(point: MonitoringPoint) {
		points.remove(point)
		point.pointSet = null
	}
}