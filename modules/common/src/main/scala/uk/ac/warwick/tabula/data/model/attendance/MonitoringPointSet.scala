package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._

import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{GeneratedId, Route}
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
@Table(name = "monitoringpointset")
class MonitoringPointSet extends GeneratedId with PermissionsTarget {

	def displayName = {
		s"${route.code.toUpperCase} ${route.name} ${
			if (year == null)
				"All years"
			else
				s"Year $year"
		}"
	}

	def shortDisplayName = {
		s"${route.code.toUpperCase} ${
			if (year == null)
				"All years"
			else
				s"Year $year"
		}"
	}

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
	var academicYear: AcademicYear = _

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "migratedto")
	var migratedTo: AttendanceMonitoringScheme = _

	def permissionsParents = Option(route).toStream

}