package uk.ac.warwick.courses.web

import uk.ac.warwick.courses.data.model

/**
 * An object that can be rendered as part of the breadcrumb navigation on the page.
 */
trait BreadCrumb {
	val title: String
	val url: String
	def linked: Boolean = true
	val tooltip: String = ""
}

object BreadCrumb {
	/**
	 * Expose Breadcrumb() method for standard breadcrumbs.
	 */
	def apply(title: String, url: String, tooltip: String = null) = Breadcrumbs.Standard(title, url, tooltip)
}

object Breadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(val title: String, val url: String, override val tooltip: String) extends Abstract

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