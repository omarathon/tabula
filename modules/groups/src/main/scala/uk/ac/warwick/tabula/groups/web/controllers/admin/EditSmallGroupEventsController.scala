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
		Mav(renderPath).crumbs(Breadcrumbs.Department(set.module.department), Breadcrumbs.Module(set.module))
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: EditSmallGroupEventsCommand
	) = render(set)

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			Redirect(Routes.admin.module(set.module))
		}
	}

}

@RequestMapping(Array("/admin/module/{module}/groups/new/{smallGroupSet}/events"))
@Controller
class CreateSmallGroupSetAddEventsController extends AbstractEditSmallGroupEventsController {

	override val renderPath = "admin/groups/newevents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.createAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.createAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.createAllocate(set))
		}
	}

}

@RequestMapping(Array("/admin/module/{module}/groups/edit/{smallGroupSet}/events"))
@Controller
class EditSmallGroupSetAddEventsController extends AbstractEditSmallGroupEventsController {

	override val renderPath = "admin/groups/editevents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.editAddStudents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.editAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.editAllocate(set))
		}
	}

}
