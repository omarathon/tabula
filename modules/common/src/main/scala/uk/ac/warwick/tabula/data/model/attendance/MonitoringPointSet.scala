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
import uk.ac.warwick.tabula.AcademicYear

@Entity
class MonitoringPointSet extends GeneratedId {
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "route_id")
	var route: Route = _
	
	@OneToMany(mappedBy = "pointSet", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@OrderBy("week")
	var points: JList[MonitoringPoint] = JArrayList()

	// Can be null, which indicates it applies to all years on this course.
	var year: JInteger = _

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())
	
	var sentToAcademicOffice: Boolean = false
	
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