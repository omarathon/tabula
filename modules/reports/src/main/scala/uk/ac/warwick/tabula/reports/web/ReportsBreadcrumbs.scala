package uk.ac.warwick.tabula.reports.web

import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.AcademicYear

trait ReportsBreadcrumbs {
	val Breadcrumbs = ReportsBreadcrumbs
}

object ReportsBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends Abstract

}