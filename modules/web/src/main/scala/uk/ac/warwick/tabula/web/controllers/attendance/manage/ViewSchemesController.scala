package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.manage.ViewSchemesCommand
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}"))
class ViewSchemesController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear)
		= ViewSchemesCommand(mandatory(department), mandatory(academicYear), user)

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringScheme]],
		@PathVariable department: Department
	): Mav = {
		val schemes = cmd.apply()

		Mav("attendance/manage/list",
			"schemes" -> schemes,
			"havePoints" -> (schemes.map{_.points.size}.sum > 0)
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department)
		)
	}

}
