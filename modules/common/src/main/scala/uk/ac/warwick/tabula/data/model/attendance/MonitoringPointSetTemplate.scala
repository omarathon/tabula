package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.AcademicYear

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("template")
class MonitoringPointSetTemplate extends AbstractMonitoringPointSet {

	@NotNull
	var templateName: String = _

	@NotNull
	var position: Int = _

	def route: Route = null
	def year: JInteger = null
	def academicYear: AcademicYear = null
}