package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.tabula.system.TwoWayConverter

class SubmissionIdConverter extends TwoWayConverter[String, Submission] {

	var service = Wire[SubmissionService]

	override def convertRight(id: String) = service.getSubmission(id).orNull
	override def convertLeft(submission: Submission) = (Option(submission) map { _.id }).orNull

}