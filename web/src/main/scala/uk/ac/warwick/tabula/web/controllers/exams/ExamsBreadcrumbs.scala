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

  object Grids {

    case object Home extends Abstract {
      val title = "Manage Exam Grids"
      val url = Some(Routes.Grids.home)
    }

    case class Department(department: model.Department, academicYear: AcademicYear) extends Abstract {
      val title: String = department.name
      val url = Some(Routes.Grids.departmentAcademicYear(department, academicYear))
    }

  }

}