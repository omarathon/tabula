package uk.ac.warwick.tabula.reports.web.controllers.attendancemonitoring

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.reports.web.ReportsBreadcrumbs
import uk.ac.warwick.tabula.reports.web.controllers.ReportsController

@Controller
@RequestMapping(Array("/{department}/{academicYear}/attendance"))
class AttendanceHomeController extends ReportsController with CurrentSITSAcademicYear {

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		Mav("attendancemonitoring/home").crumbs(
			ReportsBreadcrumbs.Home.Department(department),
			ReportsBreadcrumbs.Home.DepartmentForYear(department, academicYear)
		)
	}

}