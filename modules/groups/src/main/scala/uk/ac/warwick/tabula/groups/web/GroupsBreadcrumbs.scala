package uk.ac.warwick.tabula.groups.web

import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.data.model

trait GroupsBreadcrumbs {
	val Breadcrumbs = GroupsBreadcrumbs
}

object GroupsBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(val title: String, val url: String, override val tooltip: String) extends Abstract

	/**
	 * Special case breadcrumb for the department admin page.
	 */
	case class Department(val department: model.Department) extends Abstract {
		val title = department.name
		val url = Routes.admin(department)
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