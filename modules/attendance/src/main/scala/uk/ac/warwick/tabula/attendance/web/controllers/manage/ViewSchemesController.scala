package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.commands.manage.ViewSchemesCommand

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}"))
class ViewSchemesController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear)
		= ViewSchemesCommand(department, academicYear, user)

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringScheme]],
		@PathVariable department: Department
	) = {
		val schemes = cmd.apply()

		Mav("manage/list",
			"schemes" -> schemes,
			"havePoints" -> (schemes.map{_.points.size}.sum > 0)
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department)
		)
	}

}
