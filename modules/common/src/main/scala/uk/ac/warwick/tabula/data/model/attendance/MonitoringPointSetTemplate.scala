package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.{GeneratedId, Route}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.AcademicYear
import org.hibernate.annotations.BatchSize
import org.joda.time.DateTime

@Entity
@Table(name = "monitoringpointsettemplate")
class MonitoringPointSetTemplate extends GeneratedId {

	@NotNull
	var templateName: String = _

	@NotNull
	var position: Int = _

	@OneToMany(mappedBy = "pointSet", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@OrderBy("week")
	@BatchSize(size=100)
	var points: JList[MonitoringPointTemplate] = JArrayList()

	var createdDate: DateTime = _

	var updatedDate: DateTime = _

	def add(point: MonitoringPointTemplate) {
		points.add(point)
		point.pointSet = this
	}

	def remove(point: MonitoringPointTemplate) {
		points.remove(point)
		point.pointSet = null
	}
}