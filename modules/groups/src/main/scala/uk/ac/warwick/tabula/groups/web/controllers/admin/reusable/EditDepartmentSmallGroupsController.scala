package uk.ac.warwick.tabula.groups.web.controllers.admin.reusable

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{RequestMapping, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroupSet, DepartmentSmallGroup}
import uk.ac.warwick.tabula.groups.commands.admin.reusable.EditDepartmentSmallGroupsCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.web.Mav

abstract class AbstractEditDepartmentSmallGroupsController extends GroupsController {

	validatesSelf[SelfValidating]

	type EditDepartmentSmallGroupsCommand = Appliable[Seq[DepartmentSmallGroup]] with PopulateOnForm

	@ModelAttribute("ManageDepartmentSmallGroupsMappingParameters") def params = ManageDepartmentSmallGroupsMappingParameters

	@ModelAttribute("command") def command(@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): EditDepartmentSmallGroupsCommand =
		EditDepartmentSmallGroupsCommand(set)

	protected def render(set: DepartmentSmallGroupSet): Mav

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet,
		@ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand
	) = {
		cmd.populate()
		render(set)
	}

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			Redirect(Routes.admin.reusable(set.department))
		}
	}

}

@RequestMapping(Array("/admin/department/{department}/groups/reusable/new/{smallGroupSet}/groups"))
@Controller
class CreateDepartmentSmallGroupSetAddGroupsController extends AbstractEditDepartmentSmallGroupsController {

	override protected def render(set: DepartmentSmallGroupSet) = {
		Mav("admin/groups/reusable/newgroups")
			.crumbs(Breadcrumbs.Department(set.department))
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAddStudents))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			Redirect(Routes.admin.reusable.createAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			Redirect(Routes.admin.reusable.createAllocate(set))
		}
	}

}

@RequestMapping(Array("/admin/department/{department}/groups/reusable/edit/{smallGroupSet}/groups"))
@Controller
class EditDepartmentSmallGroupSetAddGroupsController extends AbstractEditDepartmentSmallGroupsController {

	override protected def render(set: DepartmentSmallGroupSet) = {
		Mav("admin/groups/reusable/editgroups")
			.crumbs(Breadcrumbs.Department(set.department))
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAddStudents))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			Redirect(Routes.admin.reusable.editAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			Redirect(Routes.admin.reusable.editAllocate(set))
		}
	}

}
