package uk.ac.warwick.tabula.web.web.controllers.sysadmin

import uk.ac.warwick.tabula.web.BreadCrumb

trait SysadminBreadcrumbs {
	val Breadcrumbs = SysadminBreadcrumbs
}

object SysadminBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(val title: String, val url: Option[String], override val tooltip: String) extends Abstract

	/**
	 * A breadcrumb without a link, to represent the current page.
	 * We don't currently include the current page in crumbs, but can use this for page titles
	 */
	case class Current(val title: String) extends Abstract {
		val url = None
	}
}