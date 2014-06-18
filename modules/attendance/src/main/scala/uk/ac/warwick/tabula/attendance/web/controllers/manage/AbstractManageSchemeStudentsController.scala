package uk.ac.warwick.tabula.attendance.web.controllers.manage

import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable, SelfValidating}
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage.SetStudents
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.web.Mav

abstract class AbstractManageSchemeStudentsController extends AttendanceController {

	validatesSelf[SelfValidating]

	def command(scheme: AttendanceMonitoringScheme): Appliable[AttendanceMonitoringScheme] with PopulateOnForm with SetStudents

	protected def render(scheme: AttendanceMonitoringScheme): Mav

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable scheme: AttendanceMonitoringScheme,
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme] with PopulateOnForm
	) = {
		cmd.populate()
		render(scheme)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.linkToSits))
	def linkToSits(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme] with SetStudents,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		cmd.linkToSits()
		render(scheme)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.importAsList))
	def importAsList(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme] with SetStudents,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		cmd.importAsList()
		render(scheme)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.reset))
	def reset(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		render(scheme)
	}

	@RequestMapping(method = Array(POST), params = Array("create"))
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


}
