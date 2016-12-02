package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}"))
class ViewMethodController extends AttendanceController {

	@Autowired var attendanceMonitoringService: AttendanceMonitoringService = _

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		Mav("attendance/view/viewmethod",
			"hasSchemes" -> attendanceMonitoringService.listSchemes(mandatory(department), mandatory(academicYear)).nonEmpty
		).crumbs(
			Breadcrumbs.View.Home,
			Breadcrumbs.View.Department(department)
		)
	}

}