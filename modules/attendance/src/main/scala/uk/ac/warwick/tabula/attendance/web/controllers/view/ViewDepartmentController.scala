package uk.ac.warwick.tabula.attendance.web.controllers.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.data.model.Department

/**
 * Displays the view home screen, allowing users to choose the academic year to view.
 */
@Controller
@RequestMapping(Array("/view/{department}"))
class ViewDepartmentController extends AttendanceController {

	@RequestMapping
	def home(@PathVariable department: Department) = {
		Mav("view/years", "department" -> mandatory(department)).crumbs(Breadcrumbs.View.Home)
	}

}