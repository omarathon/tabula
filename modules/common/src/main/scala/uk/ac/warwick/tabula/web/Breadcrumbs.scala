package uk.ac.warwick.tabula.web

/**
 * An object that can be rendered as part of the breadcrumb navigation on the page.
 */
trait BreadCrumb {
	val title: String
	val url: Option[String]
	def linked: Boolean = url.isDefined
	val tooltip: String = ""
}

object BreadCrumb {
	/**
	 * Expose Breadcrumb() method for standard breadcrumbs.
	 */
	def apply(title: String, url: String, tooltip: String = null) = Breadcrumbs.Standard(title, Some(url), tooltip)
}

object Breadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(val title: String, val url: Option[String], override val tooltip: String) extends Abstract
}