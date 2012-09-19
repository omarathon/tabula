package uk.ac.warwick.courses.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.system.TwoWayConverter

/**
 * Converts to and from a Department and its lowercase department code.
 */
class DepartmentCodeConverter extends TwoWayConverter[String, Department] {

	@Autowired var service: ModuleAndDepartmentService = _

	override def convertRight(code: String) = {
		service.getDepartmentByCode(sanitise(code)).getOrElse(throw new IllegalArgumentException)
	}

	override def convertLeft(department: Department) = {
		department.code
	}

	def sanitise(code: String) = {
		if (code == null) throw new IllegalArgumentException
		else code.toLowerCase
	}

}