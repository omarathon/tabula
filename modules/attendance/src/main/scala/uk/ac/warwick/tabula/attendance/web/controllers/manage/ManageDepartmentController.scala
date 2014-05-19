package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.data.model.Department

/**
 * Displays the managing home screen, allowing users to choose the department and academic year to manage.
 */
@Controller
@RequestMapping(Array("/manage/{department}"))
class ManageDepartmentController extends AttendanceController {

	@RequestMapping
	def home(@PathVariable department: Department) = {
		Mav("manage/years", "department" -> department).crumbs(Breadcrumbs.Manage.Home)
	}

}