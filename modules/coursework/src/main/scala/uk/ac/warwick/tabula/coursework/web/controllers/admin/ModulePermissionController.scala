package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod._

import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.tabula.coursework.actions.Manage
import uk.ac.warwick.tabula.coursework.commands.modules.AddModulePermissionCommand
import uk.ac.warwick.tabula.coursework.commands.modules.RemoveModulePermissionCommand
import uk.ac.warwick.tabula.coursework.data.model.Module
import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import uk.ac.warwick.tabula.coursework.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes

trait ModulePermissionControllerMethods extends BaseController {

	@ModelAttribute("addCommand") def addCommandModel = new AddModulePermissionCommand()
	@ModelAttribute("removeCommand") def removeCommandModel = new RemoveModulePermissionCommand()

	def form(module: Module): Mav = {
		checks(module)
		Mav("admin/modules/permissions/form", "module" -> module)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	def checks(module: Module) = {
		mustBeAbleTo(Manage(module))
		module.ensureParticipantsGroup
	}
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModulePermissionController extends BaseController with ModulePermissionControllerMethods {
	@RequestMapping
	def permissionsForm(@PathVariable("module") module: Module): Mav =
		form(module)
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModuleAddPermissionController extends BaseController with ModulePermissionControllerMethods {

	@RequestMapping(method = Array(POST), params = Array("_command=add"))
	def addPermission(@ModelAttribute("addCommand") command: AddModulePermissionCommand, errors: Errors): Mav = {
		val module = command.module
		checks(module)
		command.validate(errors)
		if (errors.hasErrors()) {
			form(module)
		} else {
			command.apply()
			Redirect(Routes.admin.modulePermissions(module))
		}

	}
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModuleRemovePermissionController extends BaseController with ModulePermissionControllerMethods {
	@RequestMapping(method = Array(POST), params = Array("_command=remove"))
	def addPermission(@ModelAttribute("removeCommand") command: RemoveModulePermissionCommand, errors: Errors): Mav = {
		val module = command.module
		checks(module)
		command.validate(errors)
		if (errors.hasErrors()) {
			form(module)
		} else {
			command.apply()
			Redirect(Routes.admin.modulePermissions(module))
		}

	}
}
