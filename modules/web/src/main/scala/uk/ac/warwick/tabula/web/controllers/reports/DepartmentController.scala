package uk.ac.warwick.tabula.web.controllers.reports

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.data.model.Department

@Controller
@RequestMapping(Array("/reports/{department}"))
class DepartmentController extends ReportsController with CurrentSITSAcademicYear {

	@RequestMapping
	def home(@PathVariable department: Department) = {
		Mav("reports/department",
			"academicYears" -> Seq(academicYear.previous, academicYear)
		)
	}

}