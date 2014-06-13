package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage.AddStudentsToSchemeCommand
import uk.ac.warwick.tabula.commands.Appliable
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/{scheme}/edit/students"))
class EditStudentsOnSchemeController extends AbstractManageSchemeStudentsController {

	@ModelAttribute("command")
	override def command(@PathVariable("scheme") scheme: AttendanceMonitoringScheme) =
		AddStudentsToSchemeCommand(scheme, user)

	override protected def render(scheme: AttendanceMonitoringScheme) = {
		Mav("manage/editschemestudents",
			"ManageSchemeMappingParameters" -> ManageSchemeMappingParameters
		).crumbs(
				Breadcrumbs.Manage.Home,
				Breadcrumbs.Manage.Department(scheme.department),
				Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
			)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddPoints))
	def saveAndAddPoints(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			render(scheme)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.addPointsToNewScheme(scheme))
		}

	}
}