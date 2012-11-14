package uk.ac.warwick.tabula.coursework.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.data.model.Department
import uk.ac.warwick.tabula.coursework.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.coursework.data.model.Module
import uk.ac.warwick.tabula.coursework.services.AssignmentService
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.data.model.Feedback
import uk.ac.warwick.tabula.coursework.data.FeedbackDao
import uk.ac.warwick.tabula.coursework.data.model.Submission

class SubmissionIdConverter extends Converter[String, Submission] {

	@Autowired var service: AssignmentService = _

	override def convert(id: String) = service.getSubmission(id).orNull

}