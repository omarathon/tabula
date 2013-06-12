package uk.ac.warwick.tabula.groups.web.controllers.admin

import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.groups.commands.admin.AllocateStudentsToGroupsCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.web.Mav
import org.hibernate.validator.Valid
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.controllers.BaseController

/**
 * Allocates students that are in the allocation list for groups to individual groups.
 */
@Controller
@RequestMapping(value=Array("/admin/module/{module}/groups/{set}/allocate"))
class AllocateStudentsToGroupsController extends GroupsController {
	
	validatesSelf[AllocateStudentsToGroupsCommand]
	
	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable set: SmallGroupSet) = new AllocateStudentsToGroupsCommand(module, set, user)

	@RequestMapping
	def showForm(cmd: AllocateStudentsToGroupsCommand) = {
		cmd.populate()
		cmd.sort()
		form(cmd)
	}
	
	def form(cmd: AllocateStudentsToGroupsCommand) =
		Mav("admin/groups/allocate")
		.crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))

	@RequestMapping(method=Array(POST))
	def submit(@Valid cmd:AllocateStudentsToGroupsCommand, errors: Errors): Mav = {
		cmd.sort()
		if (errors.hasErrors()) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module), "allocated" -> cmd.set.id)
		}
	}

}



