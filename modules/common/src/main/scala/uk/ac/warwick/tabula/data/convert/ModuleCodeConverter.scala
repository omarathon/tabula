package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.Promises._

class ModuleCodeConverter extends TwoWayConverter[String, Module] {

	val service = promise { Wire[ModuleAndDepartmentService] }

	override def convertRight(code: String) = 
		service.get.getModuleByCode(sanitise(code)).getOrElse {
			service.get.getModuleById(code).orNull
		}
	
	override def convertLeft(module: Module) = (Option(module) map { _.code }).orNull

	def sanitise(code: String) = {
		if (code == null) throw new IllegalArgumentException
		else code.toLowerCase
	}

}