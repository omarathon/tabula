package uk.ac.warwick.tabula.web.controllers.exams

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.BreadCrumb

trait ExamsBreadcrumbs {
  val Breadcrumbs: ExamsBreadcrumbs.type = ExamsBreadcrumbs
}

object ExamsBreadcrumbs {

  abstract class Abstract extends BreadCrumb

  case class Standard(title: String, url: Option[String], override val tooltip: String) extends Abstract

  object Exams {

    private [ExamsBreadcrumbs] class Home extends Abstract {
      val title = "Manage Exams"
      val url: Option[String] = Some(Routes.Exams.home)
    }

    private [ExamsBreadcrumbs] case class Department(department: model.Department, academicYear: Option[AcademicYear]) extends Abstract {
      val title: String = department.name
      val url: Option[String] = academicYear match {
        case Some(ay) => Some(Routes.Exams.admin.department(department, ay))
        case _ => Some(Routes.Exams.admin.department(department))
      }
    }

    private[ExamsBreadcrumbs] case class Year(dept: model.Department, academicYear: AcademicYear) extends Abstract {
      val title: String = academicYear.toString
      val url: Option[String] = Some(Routes.Exams.admin.department(dept, academicYear))
    }

    private [ExamsBreadcrumbs] case class Module(module: model.Module, academicYear: AcademicYear) extends Abstract {
      val title: String = s"${module.code.toUpperCase} ${module.name}"
      val url: Option[String] = Some(Routes.Exams.admin.module(module, academicYear))
    }

    def department(department: model.Department, academicYear: Option[AcademicYear], active: Boolean = false): Seq[BreadCrumb] =
      Seq(Department(department, academicYear))

    def module(module: model.Module, academicYear: AcademicYear): Seq[BreadCrumb] =
      department(module.adminDepartment, Some(academicYear)) ++ Seq(Year(module.adminDepartment, academicYear), Module(module, academicYear))

  }

  object Grids {

    case object Home extends Abstract {
      val title = "Manage Exam Grids"
      val url: Option[String] = Some(Routes.Grids.home)
    }

    case class Department(department: model.Department, academicYear: AcademicYear) extends Abstract {
      val title: String = department.name
      val url: Option[String] = Some(Routes.Grids.departmentAcademicYear(department, academicYear))
    }

  }

}