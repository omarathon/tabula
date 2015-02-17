package uk.ac.warwick.tabula.web.controllers.reports

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}"))
class DepartmentAndYearController extends ReportsController {

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		Mav("reports/departmentAndYear").crumbs(ReportsBreadcrumbs.Home.Department(department))
	}

}