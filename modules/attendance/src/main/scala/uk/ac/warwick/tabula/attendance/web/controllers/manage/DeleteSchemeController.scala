package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage.{DeleteSchemeCommandState, DeleteSchemeCommand}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import javax.validation.Valid
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/{scheme}/delete"))
class DeleteSchemeController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable("scheme") scheme: AttendanceMonitoringScheme) =
		DeleteSchemeCommand(scheme)

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme] with DeleteSchemeCommandState ) = {
		Mav("manage/delete")
			.crumbs(
				Breadcrumbs.Manage.Home,
				Breadcrumbs.Manage.Department(cmd.scheme.department),
				Breadcrumbs.Manage.DepartmentForYear(cmd.scheme.department, cmd.scheme.academicYear)
			)

	}

	@RequestMapping(method = Array(POST), params = Array("submit"))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme] with DeleteSchemeCommandState,
		errors: Errors
		) = {
			if (errors.hasErrors) {
				form(cmd)
			} else {
				val scheme = cmd.apply()
				Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
			}
	}

	@RequestMapping(method = Array(POST), params = Array("cancel"))
	def cancel(@RequestParam scheme: AttendanceMonitoringScheme) = {
		Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
	}

}
