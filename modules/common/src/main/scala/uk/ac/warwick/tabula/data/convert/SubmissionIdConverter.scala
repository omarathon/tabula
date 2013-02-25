package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.system.TwoWayConverter

class SubmissionIdConverter extends TwoWayConverter[String, Submission] {

	@Autowired var service: AssignmentService = _

	override def convertRight(id: String) = service.getSubmission(id).orNull
	override def convertLeft(submission: Submission) = Option(submission) map { _.id } orNull

}