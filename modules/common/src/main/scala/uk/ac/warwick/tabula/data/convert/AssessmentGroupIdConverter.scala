package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.forms.AssessmentGroup
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.helpers.Promises._

class AssessmentGroupIdConverter extends TwoWayConverter[String, AssessmentGroup] {

	val service = promise { Wire[AssignmentMembershipService] }

	// Converter used for binding request
	override def convertRight(id: String) = service.get.getAssessmentGroup(id).orNull
	
	// Formatter used for generating textual value in template
	override def convertLeft(group: AssessmentGroup) = (Option(group) map { _.id }).orNull

}
