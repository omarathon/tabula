package uk.ac.warwick.tabula.web.controllers.cm2

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.web.BreadCrumb

trait CourseworkBreadcrumbs {
	val Breadcrumbs = CourseworkBreadcrumbs
}

object CourseworkBreadcrumbs {
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends BreadCrumb

	case class Department(dept: model.Department, academicYear: Option[AcademicYear], override val active: Boolean) extends BreadCrumb {
		val title: String = dept.name
		val url: Option[String] = academicYear match {
			case Some(year) => Some(Routes.admin.department(dept, year))
			case _ => Some(Routes.admin.department(dept))
		}
	}

	object Department {
		def apply(dept: model.Department): Department =
			Department(dept, None, active = false)
		def active(dept: model.Department): Department =
			Department(dept, None, active = true)

		def apply(dept: model.Department, academicYear: AcademicYear): Department =
			Department(dept, Some(academicYear), active = false)
		def active(dept: model.Department, academicYear: AcademicYear): Department =
			Department(dept, Some(academicYear), active = true)
	}

	case class Assignment(assignment: model.Assignment, override val active: Boolean = false) extends BreadCrumb {
		val title: String = s"${assignment.name} (${assignment.module.code.toUpperCase})"
		val url: Option[String] = Some(Routes.admin.assignment.submissionsandfeedback(assignment))
	}
}