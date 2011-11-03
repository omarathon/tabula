package uk.ac.warwick.courses.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.data.model.Module

class ModuleCodeConverter extends Converter[String, Module] {
  
  @Autowired var service:ModuleAndDepartmentService =_
  
  override def convert(code:String) = service.getModuleByCode(code).orNull
  
}