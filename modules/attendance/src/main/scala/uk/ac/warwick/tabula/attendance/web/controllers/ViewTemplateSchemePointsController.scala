package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.attendance.commands.manage.{AttendancePointsFromTemplateSchemeCommandState, AttendancePointsFromTemplateSchemeCommandInternal, AttendancePointsFromTemplateSchemeCommand}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department


@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/addpoints/template/{templateScheme}"))
class ViewTemplateSchemePointsController extends BaseController {

	@ModelAttribute("command")
	def command(
		@PathVariable templateScheme: AttendanceMonitoringTemplate,
		@PathVariable academicYear: AcademicYear,
		@PathVariable department: Department
	) = {
		AttendancePointsFromTemplateSchemeCommand(templateScheme, academicYear, department)
	}

	@RequestMapping
	def getTemplateSelection(
		@ModelAttribute("command") cmd: AttendancePointsFromTemplateSchemeCommandInternal with AttendancePointsFromTemplateSchemeCommandState,
		@PathVariable templateScheme: AttendanceMonitoringTemplate,
		@PathVariable department: Department
	)	= {
		Mav("manage/_displayfindpointresults",
			"findResult" -> cmd.getGroupedPoints,
			"command" -> cmd
		).noLayoutIf(ajax)
	}
}
