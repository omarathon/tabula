package uk.ac.warwick.tabula.attendance.web

import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.data.model

trait AttendanceBreadcrumbs {
	val Breadcrumbs = AttendanceBreadcrumbs
}

object AttendanceBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends Abstract

	/**
	 * Special case breadcrumb for the department admin page.
	 */
	case class ManagingDepartment(department: model.Department) extends Abstract {
		val title = "Manage monitoring schemes"
		val url = Some(Routes.department.manage(department))
	}
}