package uk.ac.warwick.courses.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.data.FeedbackDao

class FeedbackIdConverter extends Converter[String, Feedback] {
	
  @Autowired var service:FeedbackDao =_
  
  override def convert(id:String) = service.getFeedback(id).orNull
  
}