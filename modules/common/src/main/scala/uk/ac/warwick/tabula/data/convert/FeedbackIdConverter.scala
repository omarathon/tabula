package uk.ac.warwick.tabula.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.FeedbackDao

class FeedbackIdConverter extends Converter[String, Feedback] {

	@Autowired var service: FeedbackDao = _

	override def convert(id: String) = service.getFeedback(id).orNull

}