package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModulesCommand
import uk.ac.warwick.tabula.scheduling.services.ModuleInfo
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class ModuleFixtureCommand extends CommandInternal[Unit] with Logging{

	this: ModuleAndDepartmentServiceComponent with SessionComponent=>
	import ImportModulesCommand._

  var name:String = _
	var code:String = _
	var departmentCode:String = _

	def moduleInfo = ModuleInfo(name, code,"")
	def applyInternal(){
		transactional() {
			val department  = moduleAndDepartmentService.getDepartmentByCode(departmentCode).get
			moduleAndDepartmentService.getModuleByCode(code).foreach { module =>
				department.modules.remove(module)
				session.delete(module)
				logger.info(s"Deleted module ${code}")
			}
			session.save(newModuleFrom(moduleInfo, department))
			logger.info(s"Created module ${code}")
		}
	}
}
object ModuleFixtureCommand{
	def apply()={
		new ModuleFixtureCommand()
			with ComposableCommand[Unit]
			with AutowiringModuleAndDepartmentServiceComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions

	}
}
