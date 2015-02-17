package uk.ac.warwick.tabula.web.controllers.reports

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.web.BreadCrumb

trait ReportsBreadcrumbs {
	val Breadcrumbs = ReportsBreadcrumbs
}

object ReportsBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends Abstract

	object Home {
		case class Department(department: model.Department) extends Abstract {
			val title = department.name
			val url = Some(Routes.departmentHome(department))
		}

		case class DepartmentForYear(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = academicYear.startYear.toString
			val url = Some(Routes.departmentAcademicYear(department, academicYear))
		}
	}

	object Attendance {
		case class Home(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Monitoring points"
			val url = Some(Routes.Attendance.home(department, academicYear))
		}
	}

	object SmallGroups {
		case class Home(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Small group teaching"
			val url = Some(Routes.SmallGroups.home(department, academicYear))
		}
		case class Unrecorded(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Unrecorded"
			val url = Some(Routes.SmallGroups.unrecorded(department, academicYear))
		}
		case class Missed(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Missed"
			val url = Some(Routes.SmallGroups.missed(department, academicYear))
		}
	}
}