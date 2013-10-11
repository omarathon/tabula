package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.system.TwoWayConverter

class AssessmentComponentIdConverter extends TwoWayConverter[String, AssessmentComponent] {

	@Autowired var service: AssignmentMembershipService = _

	// Converter used for binding request
	override def convertRight(id: String) = service.getAssessmentComponent(id).orNull
	
	// Formatter used for generating textual value in template
	override def convertLeft(assignment: AssessmentComponent) = (Option(assignment) map { _.id }).orNull

}