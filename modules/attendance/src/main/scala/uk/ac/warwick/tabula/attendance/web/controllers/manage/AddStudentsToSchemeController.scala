package uk.ac.warwick.tabula.attendance.web.controllers.manage

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.attendance.commands.manage._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/new/{scheme}/students"))
class AddStudentsToSchemeController extends AbstractManageSchemeStudentsController {

	@ModelAttribute("persistanceCommand")
	override def persistanceCommand(@PathVariable("scheme") scheme: AttendanceMonitoringScheme) =
		AddStudentsToSchemeCommand(mandatory(scheme), user)

	@ModelAttribute("findCommand")
	override def findCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		FindStudentsForSchemeCommand(mandatory(scheme), user)

	@ModelAttribute("editMembershipCommand")
	override def editMembershipCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		EditSchemeMembershipCommand(mandatory(scheme), user)

	override protected val renderPath = "manage/addstudentsoncreate"

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddPoints))
	def saveAndAddPoints(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with FindStudentsForSchemeCommandState,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		if (errors.hasErrors) {
			val findStudentsForSchemeCommandResult =
				if (findCommand.filterQueryString.length > 0)
					findCommand.apply()
				else
					FindStudentsForSchemeCommandResult(JArrayList(), Seq())
			val editMembershipCommandResult = editMembershipCommand.apply()
			render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.addPointsToNewScheme(scheme))
		}

	}
}
