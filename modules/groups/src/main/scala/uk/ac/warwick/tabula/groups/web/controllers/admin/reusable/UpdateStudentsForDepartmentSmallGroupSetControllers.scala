package uk.ac.warwick.tabula.groups.web.controllers.admin.reusable

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.reusable.{SetStudents, UpdateStudentsForDepartmentSmallGroupSetCommand}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.web.Mav

abstract class UpdateStudentsForDepartmentSmallGroupSetController extends GroupsController {

	validatesSelf[SelfValidating]

	type UpdateStudentsForDepartmentSmallGroupSetCommand = Appliable[DepartmentSmallGroupSet] with SetStudents

	@ModelAttribute("ManageDepartmentSmallGroupsMappingParameters") def params = ManageDepartmentSmallGroupsMappingParameters

	protected def render(set: DepartmentSmallGroupSet): Mav

	@ModelAttribute("command") def command(@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): UpdateStudentsForDepartmentSmallGroupSetCommand =
		UpdateStudentsForDepartmentSmallGroupSetCommand(set)

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet,
		@ModelAttribute("command") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand with PopulateOnForm
	) = {
		cmd.populate()
		render(set)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.linkToSits))
	def linkToSits(
		@ModelAttribute("command") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		cmd.linkToSits()
		render(set)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.importAsList))
	def importAsList(
		@ModelAttribute("command") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		cmd.importAsList()
		render(set)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.reset))
	def reset(
	  @ModelAttribute("command") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
	  @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
  ) = {
		render(set)
	}

	@RequestMapping(method = Array(POST), params = Array("create"))
	def save(
		@Valid @ModelAttribute("command") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			val set = cmd.apply()
			Redirect(Routes.admin.reusable(set.department))
		}
	}

}

@RequestMapping(Array("/admin/department/{department}/groups/reusable/new/{smallGroupSet}/students"))
@Controller
class CreateDepartmentSmallGroupSetAddStudentsController extends UpdateStudentsForDepartmentSmallGroupSetController {

	override protected def render(set: DepartmentSmallGroupSet) = {
		Mav("admin/groups/reusable/liststudents")
			.crumbs(Breadcrumbs.Department(set.department))
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAddGroups))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			val set = cmd.apply()
			Redirect(Routes.admin.reusable.createAddGroups(set))
		}
	}

}

@RequestMapping(Array("/admin/department/{department}/groups/reusable/edit/{smallGroupSet}/students"))
@Controller
class EditDepartmentSmallGroupSetAddStudentsController extends UpdateStudentsForDepartmentSmallGroupSetController {

	override protected def render(set: DepartmentSmallGroupSet) = {
		Mav("admin/groups/reusable/editstudents")
			.crumbs(Breadcrumbs.Department(set.department))
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups))
	def editAndAddGroups(
		@Valid @ModelAttribute("command") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			val set = cmd.apply()
			Redirect(Routes.admin.reusable.editAddGroups(set))
		}
	}

}