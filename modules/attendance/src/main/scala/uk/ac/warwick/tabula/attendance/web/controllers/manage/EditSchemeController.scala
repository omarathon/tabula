package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage.EditSchemeCommand

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/{scheme}/edit"))
class EditSchemeController extends AbstractManageSchemeController {

	@ModelAttribute("command")
	override def command(@PathVariable("scheme") scheme: AttendanceMonitoringScheme) =
		EditSchemeCommand(scheme, user)

	override protected def render(scheme: AttendanceMonitoringScheme) = {
		Mav("manage/edit",
			"ManageSchemeMappingParameters" -> ManageSchemeMappingParameters
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(scheme.department),
			Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
		)
	}
}
