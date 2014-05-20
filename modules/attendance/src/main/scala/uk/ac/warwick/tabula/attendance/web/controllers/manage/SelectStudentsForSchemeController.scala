package uk.ac.warwick.tabula.attendance.web.controllers.manage

import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage._
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable}
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.attendance.commands.manage.EditSchemeMembershipCommandResult
import uk.ac.warwick.tabula.attendance.commands.manage.FindStudentsForSchemeCommandResult
import collection.JavaConverters._

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/{scheme}/students/select"))
class SelectStudentsForSchemeController extends AttendanceController {

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		FindStudentsForSchemeCommand(scheme, user)

	@ModelAttribute("editMembershipCommand")
	def editMembershipCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		EditSchemeMembershipCommand(scheme, user)

	private def summaryString(
		findStudentsForSchemeCommandResult: FindStudentsForSchemeCommandResult,
		editMembershipCommandResult: EditSchemeMembershipCommandResult
	): String = {
		val sitsCount = (findStudentsForSchemeCommandResult.updatedStaticStudentIds.asScala diff editMembershipCommandResult.updatedExcludedStudentIds.asScala).size
		val removedCount = editMembershipCommandResult.updatedExcludedStudentIds.asScala.count(findStudentsForSchemeCommandResult.updatedStaticStudentIds.asScala.contains)
		val addedCount = (editMembershipCommandResult.updatedIncludedStudentIds.asScala diff findStudentsForSchemeCommandResult.updatedStaticStudentIds.asScala).size
		if (sitsCount == 0)
			""
		else
			s"$sitsCount from SITS${ removedCount match {
				case 0 => ""
				case count => s" after $count removed manually"
			}}${ addedCount match {
				case 0 => ""
				case count => s", plus $count added manually"
			}}"
	}

	private def render(
		scheme: AttendanceMonitoringScheme,
		findStudentsForSchemeCommandResult: FindStudentsForSchemeCommandResult,
		editMembershipCommandResult: EditSchemeMembershipCommandResult,
		addUsersResult: AddUsersToEditSchemeMembershipCommandResult = AddUsersToEditSchemeMembershipCommandResult(Seq(), Seq()),
		expandFind: Boolean = false,
		expandManual: Boolean = false
	) = {
		Mav("manage/selectstudents",
			"totalResults" -> 0,
			"findCommandResult" -> findStudentsForSchemeCommandResult,
			"editMembershipCommandResult" -> editMembershipCommandResult,
			"addUsersResult" -> addUsersResult,
			"summaryString" -> summaryString(findStudentsForSchemeCommandResult, editMembershipCommandResult),
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
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with AddsUsersToEditSchemeMembershipCommand,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		val addUsersResult = editMembershipCommand.addUsers()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsForSchemeCommandResult = findCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, addUsersResult = addUsersResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.manuallyExclude))
	def manuallyExclude(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with UpdatesFindStudentsForSchemeCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with RemovesUsersFromEditSchemeMembershipCommand,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		editMembershipCommand.removeUsers()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsForSchemeCommandResult = findCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.resetMembership))
	def resetMembership(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with UpdatesFindStudentsForSchemeCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with ResetsMembershipInEditSchemeMembershipCommand,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		editMembershipCommand.resetMembership()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsForSchemeCommandResult = findCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.resetAllIncluded))
	def resetAllIncluded(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with UpdatesFindStudentsForSchemeCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with ResetsMembershipInEditSchemeMembershipCommand,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		editMembershipCommand.resetAllIncluded()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsForSchemeCommandResult = findCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(CreateSchemeMappingParameters.resetAllExcluded))
	def resetAllExcluded(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with UpdatesFindStudentsForSchemeCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with ResetsMembershipInEditSchemeMembershipCommand,
		@PathVariable scheme: AttendanceMonitoringScheme
	) = {
		editMembershipCommand.resetAllExcluded()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsForSchemeCommandResult = findCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, expandManual = true)
	}

}
