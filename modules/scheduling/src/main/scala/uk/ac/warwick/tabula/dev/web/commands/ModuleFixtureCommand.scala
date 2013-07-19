package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.data.SessionComponent
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModulesCommand
import uk.ac.warwick.tabula.scheduling.services.ModuleInfo
import uk.ac.warwick.tabula.helpers.Logging

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

			moduleAndDepartmentService.getModuleByCode(code) match {
				case Some(module)=>{
					session.delete(module)
					logger.info(s"Deleted module ${code}")
				}
				case _ =>
			}
			session.save(newModuleFrom(moduleInfo, department))
			logger.info(s"Created module ${code}")
		}
	}
}
