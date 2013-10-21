package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModulesCommand
import uk.ac.warwick.tabula.scheduling.services.ModuleInfo
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.data.model.Module

class ModuleFixtureCommand extends CommandInternal[Module] with Logging{

	this: ModuleAndDepartmentServiceComponent with SessionComponent=>
	import ImportModulesCommand._

  var name:String = _
	var code:String = _
	var departmentCode:String = _

	def moduleInfo = ModuleInfo(name, code,"")
	def applyInternal() = 
		transactional() {
			val department  = moduleAndDepartmentService.getDepartmentByCode(departmentCode).get
			moduleAndDepartmentService.getModuleByCode(code).foreach { module =>
				department.modules.remove(module)
				session.delete(module)
				logger.info(s"Deleted module ${code}")
			}
			val m = newModuleFrom(moduleInfo, department)
			session.save(m)
			logger.info(s"Created module ${code}")
			m
		}
}
object ModuleFixtureCommand{
	def apply()={
		new ModuleFixtureCommand()
			with ComposableCommand[Module]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions

	}
}
