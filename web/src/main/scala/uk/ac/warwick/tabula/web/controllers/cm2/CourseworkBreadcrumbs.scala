package uk.ac.warwick.tabula.web.controllers.cm2

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.BreadCrumb

trait CourseworkBreadcrumbs {
	val Breadcrumbs = CourseworkBreadcrumbs
}

object CourseworkBreadcrumbs {

	abstract class Abstract extends BreadCrumb
	case class Standard(title: String, url: Option[String], override val tooltip: String = "") extends Abstract

	object Assignment {
		case class AssignmentManagement() extends Abstract {
			val title: String = "Assignment Management"
			val url = Some(Routes.home)
		}
	}

	object SubmissionsAndFeedback {
		case class SubmissionsAndFeedbackManagement(assignment: Assignment) extends Abstract {
			val title: String = "Submissions and Feedback Management"
			val url = Some(Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}
	object Plagiarism {

		case class PlagiarismInvestigation(val module: Module) extends Abstract {
			val title: String = "Plagiarism Investigation"
			val url = Some(Routes.admin.module(module, AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))

		}

	}
}