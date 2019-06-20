package uk.ac.warwick.tabula.web.controllers.mitcircs

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.BreadCrumb

object MitCircsBreadcrumbs {

  case class Standard(title: String, url: Option[String], override val tooltip: String) extends BreadCrumb

  object Admin {
    case class Home(department: model.Department, override val active: Boolean = false) extends BreadCrumb {
      val title: String = department.name
      val url = Some(Routes.mitcircs.Admin.home(department))
    }

    case class HomeForYear(department: model.Department, academicYear: AcademicYear, override val active: Boolean = false) extends BreadCrumb {
      val title: String = department.name
      val url = Some(Routes.mitcircs.Admin.home(department, academicYear))
    }

    case class Review(submission: model.mitcircs.MitigatingCircumstancesSubmission, override val active: Boolean = false) extends BreadCrumb {
      val title: String = s"MIT-${submission.key}"
      val url = Some(Routes.mitcircs.Admin.review(submission))
    }

    case class SensitiveEvidence(submission: model.mitcircs.MitigatingCircumstancesSubmission, override val active: Boolean = false) extends BreadCrumb {
      val title: String = "Sensitive evidence"
      val url = Some(Routes.mitcircs.Admin.sensitiveEvidence(submission))
    }

    case class AcuteOutcomes(submission: model.mitcircs.MitigatingCircumstancesSubmission, override val active: Boolean = false) extends BreadCrumb {
      val title: String = "Record acute outcomes"
      val url = Some(Routes.mitcircs.Admin.acuteOutcomes(submission))
    }

    case class PanelOutcomes(submission: model.mitcircs.MitigatingCircumstancesSubmission, override val active: Boolean = false) extends BreadCrumb {
      val title: String = "Record panel outcomes"
      val url = Some(Routes.mitcircs.Admin.panelOutcomes(submission))
    }

    case class ListPanels(department: model.Department, academicYear: AcademicYear, override val active: Boolean = false) extends BreadCrumb {
      val title: String = "Panels"
      val url = Some(Routes.mitcircs.Admin.Panels(department, academicYear))
    }

    case class Panel(panel: model.mitcircs.MitigatingCircumstancesPanel, override val active: Boolean = false) extends BreadCrumb {
      val title: String = panel.name
      val url = Some(Routes.mitcircs.Admin.Panels.view(panel))
    }
  }

}

