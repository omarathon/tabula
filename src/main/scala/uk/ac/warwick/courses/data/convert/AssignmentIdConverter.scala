package uk.ac.warwick.courses.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.data.model.Assignment

class AssignmentIdConverter extends Converter[String, Assignment] {
  
  @Autowired var service:AssignmentService =_
  
  override def convert(code:String) = service.getAssignmentById(code).orNull
  
}