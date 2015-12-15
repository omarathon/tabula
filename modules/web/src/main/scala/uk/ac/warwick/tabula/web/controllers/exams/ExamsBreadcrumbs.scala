package uk.ac.warwick.tabula.web.controllers.exams

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.BreadCrumb

trait ExamsBreadcrumbs {
	val Breadcrumbs = ExamsBreadcrumbs
}

object ExamsBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends Abstract

	/**
	 * Special case breadcrumb for the department admin page.
	 */
	case class Department(department: model.Department, academicYear: AcademicYear) extends Abstract {
		val title = department.name
		val url = Some(Routes.admin.department(department, academicYear))
	}

	/**
	 * Special case breadcrumb for a module admin page.
	 * Text is the module code, showing the name as a tooltip on hover.
	 */
	case class Module(module: model.Module, academicYear: AcademicYear) extends Abstract {
		val title = module.code.toUpperCase
		val url = Some(Routes.admin.module(module, academicYear))
		override val tooltip = module.name
	}

	/**
	 * A breadcrumb without a link, to represent the current page.
	 * We don't currently include the current page in crumbs, but can use this for page titles
	 */
	case class Current(title: String) extends Abstract {
		val url = None
	}
}