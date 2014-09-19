package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.EditSmallGroupEventsCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController

abstract class AbstractEditSmallGroupEventsController extends GroupsController {

	validatesSelf[SelfValidating]

	type EditSmallGroupEventsCommand = Appliable[SmallGroupSet]

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

	@ModelAttribute("command") def command(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): EditSmallGroupEventsCommand =
		EditSmallGroupEventsCommand(module, set)

	protected def renderPath: String

	protected def render(set: SmallGroupSet) = {
		Mav(renderPath).crumbs(Breadcrumbs.DepartmentForYear(set.module.department, set.academicYear), Breadcrumbs.ModuleForYear(set.module, set.academicYear))
	}

	@RequestMapping
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: EditSmallGroupEventsCommand
	) = render(set)

	protected def submit(cmd: EditSmallGroupEventsCommand, errors: Errors, set: SmallGroupSet, route: String) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(route)
		}
	}

	@RequestMapping(method = Array(POST), params=Array("action!=refresh"))
	def save(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin(set.module.department, set.academicYear))

}

@RequestMapping(Array("/admin/module/{module}/groups/new/{smallGroupSet}/events"))
@Controller
class CreateSmallGroupSetAddEventsController extends AbstractEditSmallGroupEventsController {

	override val renderPath = "admin/groups/newevents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndEditProperties, "action!=refresh"))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.create(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents, "action!=refresh"))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.createAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups, "action!=refresh"))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.createAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate, "action!=refresh"))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.createAllocate(set))

}

@RequestMapping(Array("/admin/module/{module}/groups/edit/{smallGroupSet}/events"))
@Controller
class EditSmallGroupSetAddEventsController extends AbstractEditSmallGroupEventsController {

	override val renderPath = "admin/groups/editevents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndEditProperties, "action!=refresh"))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.edit(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents, "action!=refresh"))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.editAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups, "action!=refresh"))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.editAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate, "action!=refresh"))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = submit(cmd, errors, set, Routes.admin.editAllocate(set))

}
