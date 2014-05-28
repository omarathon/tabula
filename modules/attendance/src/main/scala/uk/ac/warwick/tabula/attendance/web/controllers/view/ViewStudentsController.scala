package uk.ac.warwick.tabula.attendance.web.controllers.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear

// TODO This is a stub

@Controller
@RequestMapping(Array("/view/{department}/{academicYear}/students"))
class ViewStudentsController extends AttendanceController {

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		Mav("view/students"
		).crumbs(
			Breadcrumbs.View.Home,
			Breadcrumbs.View.Department(department),
			Breadcrumbs.View.DepartmentForYear(department, academicYear)
		)
	}

}