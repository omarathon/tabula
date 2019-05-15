package uk.ac.warwick.tabula.web.controllers.mitcircs

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

    case class Review(submission: model.mitcircs.MitigatingCircumstancesSubmission, override val active: Boolean = false) extends BreadCrumb {
      val title: String = s"MIT-${submission.key}"
      val url = Some(Routes.mitcircs.Admin.review(submission))
    }

    case class SensitiveEvidence(submission: model.mitcircs.MitigatingCircumstancesSubmission, override val active: Boolean = false) extends BreadCrumb {
      val title: String = "Sensitive evidence"
      val url = Some(Routes.mitcircs.Admin.sensitiveEvidence(submission))
    }
  }

}

