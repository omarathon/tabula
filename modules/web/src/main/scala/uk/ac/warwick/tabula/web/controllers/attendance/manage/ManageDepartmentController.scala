package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav

/**
 * Displays the managing home screen, allowing users to choose the department and academic year to manage.
 */
@Controller
@RequestMapping(Array("/attendance/manage/{department}"))
class ManageDepartmentController extends AttendanceController {

	@RequestMapping
	def home(@PathVariable department: Department): Mav = {
		Mav("attendance/manage/years", "department" -> mandatory(department)).crumbs(Breadcrumbs.Manage.Home)
	}

}