package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.JavaImports._
import javax.persistence._
import uk.ac.warwick.tabula.data.model.{GeneratedId, Route}
import org.joda.time.DateTime
import org.hibernate.annotations.{BatchSize, Type}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
@Table(name = "monitoringpointset")
class MonitoringPointSet extends GeneratedId with PermissionsTarget {

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
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "route_id")
	var route: Route = _

	// Can be null, which indicates it applies to all years on this course.
	var year: JInteger = _

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())
	
	def permissionsParents = Option(route).toStream

}