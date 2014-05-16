package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.attendance.commands.manage._
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.ProfileService

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/new/{scheme}/students"))
class AddStudentsToSchemeController extends AttendanceController {

	@Autowired var profileService: ProfileService = _

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable scheme: AttendanceMonitoringScheme) =
		AddStudentsToSchemeCommand(scheme)

	private def render(scheme: AttendanceMonitoringScheme) = {
		Mav("manage/liststudents",
			"CreateSchemeMappingParameters" -> CreateSchemeMappingParameters
		).crumbs(
				Breadcrumbs.Manage.Home,
				Breadcrumbs.Manage.Department(scheme.department),
				Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
			)
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(@PathVariable scheme: AttendanceMonitoringScheme) = {
		render(scheme)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.linkToSits))
	def linkToSits(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme] with SetStudents,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		cmd.linkToSits()
		render(scheme)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.importAsList))
	def importAsList(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme] with SetStudents,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		cmd.importAsList()
		render(scheme)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.reset))
	def reset(@PathVariable scheme: AttendanceMonitoringScheme) = {
		render(scheme)
	}

	@RequestMapping(method = Array(POST), params = Array("create"))
	def save(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			form(scheme)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.createAndAddPoints))
	def saveAndAddPoints(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			form(scheme)
		} else {
			val scheme = cmd.apply()
			// TODO change to wherever the add points path is
			Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
		}

	}

}
