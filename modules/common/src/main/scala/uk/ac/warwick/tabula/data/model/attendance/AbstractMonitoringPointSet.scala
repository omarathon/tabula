package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.{Route, GeneratedId}
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import org.joda.time.DateTime
import org.hibernate.annotations.{Type, BatchSize}
import uk.ac.warwick.tabula.AcademicYear

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

	def route: Route
	def year: JInteger
	def academicYear: AcademicYear
}