package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage.EditSchemeCommand
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.{SelfValidating, PopulateOnForm, Appliable}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/{scheme}/edit"))
class EditSchemeController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable("scheme") scheme: AttendanceMonitoringScheme) =
		EditSchemeCommand(scheme, user)

	private def render(scheme: AttendanceMonitoringScheme) = {
		Mav("manage/edit",
			"ManageSchemeMappingParameters" -> ManageSchemeMappingParameters
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(scheme.department),
			Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
		)
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable scheme: AttendanceMonitoringScheme,
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme] with PopulateOnForm
	) = {
		cmd.populate()
		render(scheme)
	}

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			render(scheme)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddStudents))
	def saveAndEditStudents(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable("scheme") scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			render(scheme)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.editSchemeStudents(scheme))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddPoints))
	def saveAndEditPoints(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable("scheme") scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			render(scheme)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.editSchemePoints(scheme))
		}
	}
}
