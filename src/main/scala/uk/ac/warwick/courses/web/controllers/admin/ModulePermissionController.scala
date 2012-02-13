package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.JavaImports._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestMethod._
import org.codehaus.jackson.map.annotate.JsonView
import uk.ac.warwick.courses.actions.Manage
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.courses.commands.modules.AddModulePermissionCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.data.model.UserGroup
import org.springframework.validation.Errors
import uk.ac.warwick.courses.commands.modules.RemoveModulePermissionCommand

trait ModulePermissionControllerMethods extends BaseController {
	
	@ModelAttribute("addCommand") def addCommandModel = new AddModulePermissionCommand()
	@ModelAttribute("removeCommand") def removeCommandModel = new RemoveModulePermissionCommand()
	
	def form(module:Module) : Mav = {
		checks(module)
		Mav("admin/modules/permissions/form", "module"->module)
	}
	
	def checks(module:Module) = {
		mustBeAbleTo(Manage(module))
		module.ensureParticipantsGroup
	}
}

@Controller @RequestMapping(value=Array("/admin/module/{module}/permissions"))
class ModulePermissionController extends BaseController with ModulePermissionControllerMethods {
	@RequestMapping
	def permissionsForm(@PathVariable("module") module:Module) : Mav = 
		form(module)
}

@Controller @RequestMapping(value=Array("/admin/module/{module}/permissions"))
class ModuleAddPermissionController extends BaseController with ModulePermissionControllerMethods {
	
	@RequestMapping(method=Array(POST), params=Array("_command=add"))
	def addPermission( @ModelAttribute("addCommand") command:AddModulePermissionCommand, errors:Errors ) : Mav = {
		val module = command.module
		checks(module)
		command.validate(errors)
		if (errors.hasErrors()) {
			form(module)
		} else {
			command.apply()
			Redirect("/admin/module/"+module.code+"/permissions")
		}
		
	}
}

@Controller @RequestMapping(value=Array("/admin/module/{module}/permissions"))
class ModuleRemovePermissionController extends BaseController with ModulePermissionControllerMethods {	
	@RequestMapping(method=Array(POST), params=Array("_command=remove"))
	def addPermission( @ModelAttribute("removeCommand") command:RemoveModulePermissionCommand, errors:Errors ) : Mav = {
		val module = command.module
		checks(module)
		command.validate(errors)
		if (errors.hasErrors()) {
			form(module)
		} else {
			command.apply()
			Redirect("/admin/module/"+module.code+"/permissions")
		}
		
	}
}
