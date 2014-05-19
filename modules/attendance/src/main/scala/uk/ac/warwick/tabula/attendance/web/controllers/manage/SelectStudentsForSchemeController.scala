package uk.ac.warwick.tabula.attendance.web.controllers.manage

import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage.{UpdatesFindStudentsForSchemeCommand, FindStudentsForSchemeCommandState, FindStudentsForSchemeCommandResult, EditSchemeMembershipCommandResult, FindStudentsForSchemeCommand, EditSchemeMembershipCommand}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable}
import uk.ac.warwick.tabula.JavaImports.JArrayList

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/{scheme}/students/select"))
class SelectStudentsForSchemeController extends AttendanceController {

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		FindStudentsForSchemeCommand(scheme, user)

	@ModelAttribute("editMembershipCommand")
	def editMembershipCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		EditSchemeMembershipCommand(scheme, user)

	private def render(
		scheme: AttendanceMonitoringScheme,
		findStudentsForSchemeCommandResult: FindStudentsForSchemeCommandResult,
		editMembershipCommandResult: EditSchemeMembershipCommandResult,
		expandFind: Boolean = false,
		expandManual: Boolean = false
	) = {
		Mav("manage/selectstudents",
			"totalResults" -> 0,
			"findCommandResult" -> findStudentsForSchemeCommandResult,
			"editMembershipCommandResult" -> editMembershipCommandResult,
			"expandFind" -> expandFind,
			"expandManual" -> expandManual,
			"CreateSchemeMappingParameters" -> CreateSchemeMappingParameters
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(scheme.department),
			Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def form(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with PopulateOnForm with FindStudentsForSchemeCommandState,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with PopulateOnForm,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		findCommand.populate()
		editMembershipCommand.populate()
		val findStudentsForSchemeCommandResult =
			if (findCommand.filterQueryString.length > 0)
				findCommand.apply()
			else
				FindStudentsForSchemeCommandResult(JArrayList(), Seq())
		val editMembershipCommandResult = editMembershipCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.findStudents))
	def findStudents(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult],
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		val findStudentsForSchemeCommandResult = findCommand.apply()
		val editMembershipCommandResult = editMembershipCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, expandFind = true)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.manuallyAddForm))
	def manuallyAddForm(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult],
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		Mav("manage/manuallyaddstudents",
			"CreateSchemeMappingParameters" -> CreateSchemeMappingParameters
		).crumbs(
				Breadcrumbs.Manage.Home,
				Breadcrumbs.Manage.Department(scheme.department),
				Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
			)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.manuallyAddSubmit))
	def manuallyAddSubmit(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with UpdatesFindStudentsForSchemeCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsForSchemeCommandResult = findCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, expandManual = true)
	}

}
