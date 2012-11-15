package uk.ac.warwick.tabula.coursework.web

import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.data.model

trait CourseworkBreadcrumbs {
	val Breadcrumbs = CourseworkBreadcrumbs
}

object CourseworkBreadcrumbs extends uk.ac.warwick.tabula.web.Breadcrumbs {
	/**
	 * Special case breadcrumb for the department admin page.
	 */
	case class Department(val department: model.Department) extends Abstract {
		val title = department.name
		val url = Routes.admin.department(department)
	}

	/**
	 * Special case breadcrumb for a module admin page.
	 * Text is the module code, showing the name as a tooltip on hover.
	 */
	case class Module(val module: model.Module) extends Abstract {
		val title = module.code.toUpperCase
		val url = Routes.admin.module(module)
		override val tooltip = module.name
	}

	/**
	 * Not current used: a breadcrumb without a link, to represent
	 * the current page. We don't currently include the current page in crumbs.
	 */
	case class Current(val title: String) extends Abstract {
		val url = null
		override def linked = false
	}
}