package uk.ac.warwick.tabula.reports.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.data.model.Department

@Controller
@RequestMapping(Array("/{department}"))
class DepartmentController extends ReportsController with CurrentSITSAcademicYear {

	@RequestMapping
	def home(@PathVariable department: Department) = {
		Mav("department",
			"academicYears" -> Seq(academicYear.previous, academicYear)
		)
	}

}