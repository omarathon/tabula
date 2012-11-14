package uk.ac.warwick.tabula.coursework.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.data.model.Department
import uk.ac.warwick.tabula.coursework.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.coursework.data.model.Module

class ModuleCodeConverter extends Converter[String, Module] {

	@Autowired var service: ModuleAndDepartmentService = _

	override def convert(code: String) = service.getModuleByCode(code).getOrElse(throw new IllegalArgumentException)

}