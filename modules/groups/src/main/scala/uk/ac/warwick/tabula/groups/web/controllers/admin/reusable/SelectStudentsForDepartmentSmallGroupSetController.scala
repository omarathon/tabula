package uk.ac.warwick.tabula.groups.web.controllers.admin.reusable

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable}
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.reusable._
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._

@Controller
@RequestMapping(Array("/admin/department/{department}/groups/reusable/edit/{smallGroupSet}/students/select"))
class SelectStudentsForDepartmentSmallGroupSetController extends GroupsController {

	type FindStudentsForDepartmentSmallGroupSetCommand = Appliable[FindStudentsForDepartmentSmallGroupSetCommandResult] with PopulateOnForm with FindStudentsForDepartmentSmallGroupSetCommandState with UpdatesFindStudentsForDepartmentSmallGroupSetCommand
	type EditDepartmentSmallGroupSetMembershipCommand = Appliable[EditDepartmentSmallGroupSetMembershipCommandResult] with PopulateOnForm with AddsUsersToEditDepartmentSmallGroupSetMembershipCommand with RemovesUsersFromEditDepartmentSmallGroupSetMembershipCommand with ResetsMembershipInEditDepartmentSmallGroupSetMembershipCommand

	@ModelAttribute("ManageDepartmentSmallGroupsMappingParameters") def params = ManageDepartmentSmallGroupsMappingParameters

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): FindStudentsForDepartmentSmallGroupSetCommand =
		FindStudentsForDepartmentSmallGroupSetCommand(mandatory(set))

	@ModelAttribute("editMembershipCommand")
	def editMembershipCommand(@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): EditDepartmentSmallGroupSetMembershipCommand =
		EditDepartmentSmallGroupSetMembershipCommand(mandatory(set))

	private def summaryString(
		findStudentsCommandResult: FindStudentsForDepartmentSmallGroupSetCommandResult,
		editMembershipCommandResult: EditDepartmentSmallGroupSetMembershipCommandResult
	): String = {
		val sitsCount = (findStudentsCommandResult.updatedStaticStudentIds.asScala diff editMembershipCommandResult.updatedExcludedStudentIds.asScala).size
		val removedCount = editMembershipCommandResult.updatedExcludedStudentIds.asScala.count(findStudentsCommandResult.updatedStaticStudentIds.asScala.contains)
		val addedCount = (editMembershipCommandResult.updatedIncludedStudentIds.asScala diff findStudentsCommandResult.updatedStaticStudentIds.asScala).size
		if (sitsCount == 0)
			""
		else
			s"$sitsCount from SITS${removedCount match {
				case 0 => ""
				case count => s" after $count removed manually"
			}}${addedCount match {
				case 0 => ""
				case count => s", plus $count added manually"
			}}"
	}

	private def render(
		set: DepartmentSmallGroupSet,
		findStudentsCommandResult: FindStudentsForDepartmentSmallGroupSetCommandResult,
		editMembershipCommandResult: EditDepartmentSmallGroupSetMembershipCommandResult,
		addUsersResult: AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult = AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult(Seq()),
		expandFind: Boolean = false,
		expandManual: Boolean = false
	) = {
		Mav("admin/groups/reusable/selectstudents",
			"totalResults" -> 0,
			"findCommandResult" -> findStudentsCommandResult,
			"editMembershipCommandResult" -> editMembershipCommandResult,
			"addUsersResult" -> addUsersResult,
			"summaryString" -> summaryString(findStudentsCommandResult, editMembershipCommandResult),
			"expandFind" -> expandFind,
			"expandManual" -> expandManual,
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Department(set.department)
		)
	}

	@RequestMapping(method = Array(POST))
	def form(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		findCommand.populate()
		editMembershipCommand.populate()
		val findStudentsCommandResult =
			if (findCommand.filterQueryString.length > 0)
				findCommand.apply()
			else
				FindStudentsForDepartmentSmallGroupSetCommandResult(JArrayList(), Seq())
		val editMembershipCommandResult = editMembershipCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.findStudents))
	def findStudents(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		val findStudentsCommandResult = findCommand.apply()
		val editMembershipCommandResult = editMembershipCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult, expandFind = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.manuallyAddForm))
	def manuallyAddForm(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		Mav("admin/groups/reusable/manuallyaddstudents",
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Department(set.department)
		)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.manuallyAddSubmit))
	def manuallyAddSubmit(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		val addUsersResult = editMembershipCommand.addUsers()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsCommandResult = findCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult, addUsersResult = addUsersResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.manuallyExclude))
	def manuallyExclude(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		editMembershipCommand.removeUsers()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsCommandResult = findCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.resetMembership))
	def resetMembership(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		editMembershipCommand.resetMembership()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsCommandResult = findCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult, expandManual = true)
	}

}
