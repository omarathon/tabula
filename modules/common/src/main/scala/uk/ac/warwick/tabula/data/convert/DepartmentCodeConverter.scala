package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.Promises._

/**
 * Converts to and from a Department and its lowercase department code.
 */
class DepartmentCodeConverter extends TwoWayConverter[String, Department] {

	val service = promise { Wire[ModuleAndDepartmentService] }

	override def convertRight(code: String) = {
		service.get.getDepartmentByCode(sanitise(code)).getOrElse {
			service.get.getDepartmentById(code).orNull
		}
	}

	override def convertLeft(department: Department) = (Option(department) map { _.code }).orNull

	def sanitise(code: String) = {
		if (code == null) throw new IllegalArgumentException
		else code.toLowerCase
	}

}