package uk.ac.warwick.courses.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.services.AssignmentService

class UpstreamAssignmentIdConverter extends Converter[String, UpstreamAssignment] {
  
  @Autowired var service:AssignmentService =_
  
  override def convert(id:String) = service.getUpstreamAssignment(id).orNull
  
}