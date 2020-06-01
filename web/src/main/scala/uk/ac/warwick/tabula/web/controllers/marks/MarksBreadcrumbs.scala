package uk.ac.warwick.tabula.web.controllers.marks

import java.text.DecimalFormat

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

    case class AssessmentComponentMissingMarks(assessmentComponent: model.AssessmentComponent, upstreamAssessmentGroup: model.UpstreamAssessmentGroup, override val active: Boolean = false) extends BreadCrumb {
      val title: String = s"${assessmentComponent.name} (${upstreamAssessmentGroup.academicYear.toString})"
      val url = Some(Routes.marks.Admin.AssessmentComponents.missingMarks(assessmentComponent, upstreamAssessmentGroup))
    }

    case class AssessmentComponentScaling(assessmentComponent: model.AssessmentComponent, upstreamAssessmentGroup: model.UpstreamAssessmentGroup, override val active: Boolean = false) extends BreadCrumb {
      val title: String = s"${assessmentComponent.name} (${upstreamAssessmentGroup.academicYear.toString})"
      val url = Some(Routes.marks.Admin.AssessmentComponents.scaling(assessmentComponent, upstreamAssessmentGroup))
    }

    case class ModuleOccurrenceRecordMarks(module: model.Module, cats: BigDecimal, academicYear: AcademicYear, occurrence: String, override val active: Boolean = false) extends BreadCrumb {
      val title: String = s"${module.code.toUpperCase}-${new DecimalFormat("0.#").format(cats.setScale(1, BigDecimal.RoundingMode.HALF_UP))} ${module.name} (${academicYear.toString}, $occurrence)"
      val url = Some(Routes.marks.Admin.ModuleOccurrences.recordMarks(module, cats, academicYear, occurrence))
    }
  }

}

