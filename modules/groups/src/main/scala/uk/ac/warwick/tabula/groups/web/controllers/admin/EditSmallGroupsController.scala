package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.commands.admin.EditSmallGroupsCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController

abstract class AbstractEditSmallGroupsController extends GroupsController {

	validatesSelf[SelfValidating]

	type EditSmallGroupsCommand = Appliable[Seq[SmallGroup]] with PopulateOnForm

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

	@ModelAttribute("command") def command(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): EditSmallGroupsCommand =
		EditSmallGroupsCommand(module, set)

	protected def renderPath: String

	protected def render(set: SmallGroupSet) = {
		Mav(renderPath).crumbs(Breadcrumbs.Department(set.module.department), Breadcrumbs.Module(set.module))
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: EditSmallGroupsCommand
	) = {
		cmd.populate()
		render(set)
	}

	protected def submit(cmd: EditSmallGroupsCommand, errors: Errors, set: SmallGroupSet, route: String) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(route)
		}
	}

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.module(set.module))

}

@RequestMapping(Array("/admin/module/{module}/groups/new/{smallGroupSet}/groups"))
@Controller
class CreateSmallGroupSetAddGroupsController extends AbstractEditSmallGroupsController {

	override val renderPath = "admin/groups/newgroups"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndEditProperties))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.create(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.createAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddEvents))
	def saveAndAddEvents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.createAddEvents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.createAllocate(set))

}

@RequestMapping(Array("/admin/module/{module}/groups/edit/{smallGroupSet}/groups"))
@Controller
class EditSmallGroupSetAddGroupsController extends AbstractEditSmallGroupsController {

	override val renderPath = "admin/groups/editgroups"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndEditProperties))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.edit(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.editAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddEvents))
	def saveAndAddEvents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.editAddEvents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.editAllocate(set))

}
