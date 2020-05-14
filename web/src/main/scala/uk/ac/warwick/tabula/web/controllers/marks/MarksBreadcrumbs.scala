package uk.ac.warwick.tabula.web.controllers.marks

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

object MarksBreadcrumbs {

  case class Standard(title: String, url: Option[String], override val tooltip: String) extends BreadCrumb

  object Admin {
    case class Home(department: model.Department, override val active: Boolean = false) extends BreadCrumb {
      val title: String = department.name
      val url = Some(Routes.marks.Admin.home(department))
    }

    case class HomeForYear(department: model.Department, academicYear: AcademicYear, override val active: Boolean = false) extends BreadCrumb {
      val title: String = department.name
      val url = Some(Routes.marks.Admin.home(department, academicYear))
    }

    case class AssessmentComponents(department: model.Department, academicYear: AcademicYear, override val active: Boolean = false) extends BreadCrumb {
      val title: String = "Assessment Components"
      val url = Some(Routes.marks.Admin.AssessmentComponents(department, academicYear))
    }

    case class AssessmentComponentRecordMarks(assessmentComponent: model.AssessmentComponent, upstreamAssessmentGroup: model.UpstreamAssessmentGroup, override val active: Boolean = false) extends BreadCrumb {
      val title: String = s"${assessmentComponent.name} (${upstreamAssessmentGroup.academicYear.toString})"
      val url = Some(Routes.marks.Admin.AssessmentComponents.recordMarks(assessmentComponent, upstreamAssessmentGroup))
    }
  }

}

