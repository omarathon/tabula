package uk.ac.warwick.tabula.data.model

import javax.persistence.{Entity, FetchType, JoinColumn, ManyToOne}

import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._

@Entity
class CoreRequiredModule extends GeneratedId {

	def this(route: Route, academicYear: AcademicYear, yearOfStudy: Int, module: Module) {
		this()
		this.route = route
		this.academicYear = academicYear
		this.yearOfStudy = yearOfStudy
		this.module = module
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "routeCode", referencedColumnName="code")
	var route: Route = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var academicYear: AcademicYear = null

	var yearOfStudy: JInteger = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="moduleCode", referencedColumnName="code")
	var module: Module = null

}
