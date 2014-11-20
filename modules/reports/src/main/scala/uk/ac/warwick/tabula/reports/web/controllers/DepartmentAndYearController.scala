package uk.ac.warwick.tabula.reports.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.reports.web.ReportsBreadcrumbs

@Controller
@RequestMapping(Array("/{department}/{academicYear}"))
class DepartmentAndYearController extends ReportsController {

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		Mav("departmentAndYear").crumbs(ReportsBreadcrumbs.Home.Department(department))
	}

}