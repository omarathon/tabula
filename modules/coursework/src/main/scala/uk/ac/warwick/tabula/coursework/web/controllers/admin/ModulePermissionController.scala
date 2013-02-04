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
import uk.ac.warwick.tabula.coursework.commands.modules.AddModulePermissionCommand
import uk.ac.warwick.tabula.coursework.commands.modules.RemoveModulePermissionCommand
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes

trait ModulePermissionControllerMethods extends CourseworkController {

	@ModelAttribute("addCommand") def addCommandModel(@PathVariable("module") module: Module) = new AddModulePermissionCommand(module)
	@ModelAttribute("removeCommand") def removeCommandModel(@PathVariable("module") module: Module) = new RemoveModulePermissionCommand(module)

	def form(module: Module): Mav = {
		Mav("admin/modules/permissions/form", "module" -> module)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModulePermissionController extends CourseworkController with ModulePermissionControllerMethods {
	@RequestMapping
	def permissionsForm(@PathVariable("module") module: Module): Mav =
		form(module)
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModuleAddPermissionController extends CourseworkController with ModulePermissionControllerMethods {

	@RequestMapping(method = Array(POST), params = Array("_command=add"))
	def addPermission(@ModelAttribute("addCommand") command: AddModulePermissionCommand, errors: Errors): Mav = {
		val module = command.module
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
class ModuleRemovePermissionController extends CourseworkController with ModulePermissionControllerMethods {
	@RequestMapping(method = Array(POST), params = Array("_command=remove"))
	def addPermission(@ModelAttribute("removeCommand") command: RemoveModulePermissionCommand, errors: Errors): Mav = {
		val module = command.module
		command.validate(errors)
		if (errors.hasErrors()) {
			form(module)
		} else {
			command.apply()
			Redirect(Routes.admin.modulePermissions(module))
		}

	}
}
