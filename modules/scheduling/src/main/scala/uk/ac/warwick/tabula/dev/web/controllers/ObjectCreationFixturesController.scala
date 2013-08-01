package uk.ac.warwick.tabula.dev.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import org.springframework.web.bind.annotation.RequestMethod.POST
import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, Appliable}
import uk.ac.warwick.tabula.dev.web.commands.{SmallGroupSetFixtureCommand, ModuleFixtureCommand}
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

@Controller
@RequestMapping(Array("/fixtures/create/module"))
class ModuleCreationFixturesController {

	@ModelAttribute("createModuleCommand")
	def getCreateModuleCommand(): Appliable[Unit] = {
		new ModuleFixtureCommand()
			with ComposableCommand[Unit]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions

	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createModuleCommand") cmd: Appliable[Unit]) = {
		cmd.apply()
	}
}

@Controller
@RequestMapping(Array("/fixtures/create/groupset"))
class SmallGroupSetCreationFixturesController {

	@ModelAttribute("createGroupSetCommand")
	def getCreateModuleCommand(): Appliable[Unit] = {
		new SmallGroupSetFixtureCommand()
			with ComposableCommand[Unit]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions

	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("createGroupSetCommand") cmd: Appliable[Unit]) = {
		cmd.apply()
	}
}


