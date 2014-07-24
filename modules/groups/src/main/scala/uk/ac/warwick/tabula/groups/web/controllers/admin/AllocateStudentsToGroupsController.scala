package uk.ac.warwick.tabula.groups.web.controllers.admin

import uk.ac.warwick.tabula.commands.{GroupsObjects, Appliable, SelfValidating}
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.groups.commands.admin.AllocateStudentsToGroupsCommand
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.userlookup.User

/**
 * Allocates students that are in the allocation list for groups to individual groups.
 */
abstract class AbstractAllocateStudentsToGroupsController extends GroupsController {

	validatesSelf[SelfValidating]
	type AllocateStudentsToGroupsCommand = Appliable[SmallGroupSet] with GroupsObjects[User, SmallGroup]

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

	@ModelAttribute("command")
	def command(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") smallGroupSet: SmallGroupSet): AllocateStudentsToGroupsCommand =
		AllocateStudentsToGroupsCommand(module, smallGroupSet, user)

	@RequestMapping
	def showForm(@ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, @PathVariable("module") module: Module) = {
		cmd.populate()
		cmd.sort()
		form(cmd, module)
	}

	protected val renderPath: String

	protected def form(cmd: AllocateStudentsToGroupsCommand, module: Module) =
		Mav(renderPath).crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("module") module: Module, @PathVariable("smallGroupSet") smallGroupSet: SmallGroupSet): Mav = {
		cmd.sort()
		if (errors.hasErrors()) {
			form(cmd, module)
		} else {
			cmd.apply()
			Redirect(Routes.admin.module(module), "allocated" -> smallGroupSet.id)
		}
	}

}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/groups/{smallGroupSet}/allocate"))
class AllocateStudentsToGroupsController extends AbstractAllocateStudentsToGroupsController {
	override protected val renderPath = "admin/groups/allocate"
}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/groups/new/{smallGroupSet}/allocate"))
class CreateSmallGroupSetAllocateController extends AbstractAllocateStudentsToGroupsController {
	override protected val renderPath = "admin/groups/newallocate"
}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/groups/edit/{smallGroupSet}/allocate"))
class EditSmallGroupSetAllocateController extends AbstractAllocateStudentsToGroupsController {
	override protected val renderPath = "admin/groups/editallocate"
}