package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.forms.AssessmentGroup
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.services.AssignmentMembershipService

class AssessmentGroupIdConverter extends TwoWayConverter[String, AssessmentGroup] {

	var service = Wire[AssignmentMembershipService]

	// Converter used for binding request
	override def convertRight(id: String) = service.getAssessmentGroup(id).orNull
	
	// Formatter used for generating textual value in template
	override def convertLeft(group: AssessmentGroup) = (Option(group) map { _.id }).orNull

}
