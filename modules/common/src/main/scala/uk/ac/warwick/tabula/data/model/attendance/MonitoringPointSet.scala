package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.JavaImports._
import javax.persistence._
import uk.ac.warwick.tabula.data.model.Route
import org.joda.time.DateTime
import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.AcademicYear

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("normal")
class MonitoringPointSet extends AbstractMonitoringPointSet {
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "route_id")
	var route: Route = _

	// Can be null, which indicates it applies to all years on this course.
	var year: JInteger = _

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())
	
	var sentToAcademicOffice: Boolean = false
}