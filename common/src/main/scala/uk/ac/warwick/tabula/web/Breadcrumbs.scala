package uk.ac.warwick.tabula.web

import uk.ac.warwick.tabula.AcademicYear

/**
  * An object that can be rendered as part of the breadcrumb navigation on the page.
  */
trait BreadCrumb {
  val title: String
  val url: Option[String]

  def linked: Boolean = url.isDefined

  val tooltip: String = ""
  val active: Boolean = false // ID7 navigation active

  val scopedAcademicYear: Option[AcademicYear] = None

  def isAcademicYearScoped: Boolean = scopedAcademicYear.isDefined
}

trait Activeable[T] {
  def setActive(thisActive: T): BreadCrumb
}

trait ActiveableByBoolean extends Activeable[Boolean]

object BreadCrumb {
  /**
    * Expose Breadcrumb() method for standard breadcrumbs.
    */
  def apply(
    title: String,
    url: String,
    tooltip: String = null,
    active: Boolean = false,
  ) = Breadcrumbs.Standard(title, Some(url), tooltip, active)
}

object Breadcrumbs {

  case class Standard(
    title: String,
    url: Option[String],
    override val tooltip: String,
    override val active: Boolean = false,
  ) extends BreadCrumb with ActiveableByBoolean {
    override def setActive(thisActive: Boolean): BreadCrumb = {
      if (this.active && !thisActive) this.copy(active = thisActive) else this
    }
  }

  case class AcademicYearScoped(
    title: String,
    url: Option[String],
    override val tooltip: String,
    override val scopedAcademicYear: Option[AcademicYear],
    override val active: Boolean = false,
  ) extends BreadCrumb with ActiveableByBoolean {
    override def setActive(thisActive: Boolean): BreadCrumb = {
      if (this.active && !thisActive) this.copy(active = thisActive) else this
    }
  }

}