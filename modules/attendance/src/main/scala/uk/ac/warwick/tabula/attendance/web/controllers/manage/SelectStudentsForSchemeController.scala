package uk.ac.warwick.tabula.attendance.web.controllers.manage

import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage.{FindStudentsForSchemeCommand, EditSchemeMembershipCommand}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable}

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/{scheme}/students/select"))
class SelectStudentsForSchemeController extends AttendanceController {

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		FindStudentsForSchemeCommand(scheme, user)

	@ModelAttribute("editMembershipCommand")
	def editMembershipCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		EditSchemeMembershipCommand(scheme)

	private def render(scheme: AttendanceMonitoringScheme) = {
		Mav("manage/selectstudents",
			"totalResults" -> 0,
			"linkToSitsString" -> CreateSchemeMappingParameters.linkToSitsString,
			"importAsListString" -> CreateSchemeMappingParameters.importAsListString,
			"resetString" -> CreateSchemeMappingParameters.resetString,
			"manuallyAddFormString" -> CreateSchemeMappingParameters.manuallyAddFormString
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(scheme.department),
			Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def form(
		@ModelAttribute("findCommand") findCommand: PopulateOnForm,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: PopulateOnForm,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		findCommand.populate()
		editMembershipCommand.populate()
		render(scheme)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.manuallyAddFormString))
	def manuallyAddForm(@PathVariable scheme: AttendanceMonitoringScheme) = {
		Mav("manage/manuallyaddstudents",
			"manuallyAddSubmitString" -> CreateSchemeMappingParameters.manuallyAddSubmitString
		).crumbs(
				Breadcrumbs.Manage.Home,
				Breadcrumbs.Manage.Department(scheme.department),
				Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
			)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.manuallyAddSubmitString))
	def manuallyAddSubmit(@ModelAttribute("editMembershipCommand") cmd: Appliable[AttendanceMonitoringScheme]) = {
		val scheme = cmd.apply()
		render(scheme)
	}

}
