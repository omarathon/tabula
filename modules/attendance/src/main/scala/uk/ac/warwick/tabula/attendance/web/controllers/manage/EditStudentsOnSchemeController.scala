package uk.ac.warwick.tabula.attendance.web.controllers.manage

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.commands.manage._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/{scheme}/edit/students"))
class EditStudentsOnSchemeController extends AbstractManageSchemeStudentsController {

	override protected val renderPath = "manage/editschemestudents"

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddPoints))
	def saveAndEditPoints(
		@Valid @ModelAttribute("persistanceCommand") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with FindStudentsForSchemeCommandState,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			val findStudentsForSchemeCommandResult = findCommand.apply()
			val editMembershipCommandResult = editMembershipCommand.apply()
			render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult)
		} else {
			val scheme = cmd.apply()
			RedirectForce(Routes.Manage.editSchemePoints(scheme))
		}

	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.saveAndEditProperties))
	def saveAndEditProperties(
		@Valid @ModelAttribute("persistanceCommand") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with FindStudentsForSchemeCommandState,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			val findStudentsForSchemeCommandResult = findCommand.apply()
			val editMembershipCommandResult = editMembershipCommand.apply()
			render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult)
		} else {
			val scheme = cmd.apply()
			RedirectForce(Routes.Manage.editScheme(scheme))
		}

	}
}