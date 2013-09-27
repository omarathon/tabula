package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.AssessmentGroup
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.services.AssignmentMembershipService

class AssessmentGroupIdConverter extends TwoWayConverter[String, AssessmentGroup] {

	@Autowired var service: AssignmentMembershipService = _

	// Converter used for binding request
	override def convertRight(id: String) = service.getAssessmentGroup(id).orNull
	
	// Formatter used for generating textual value in template
	override def convertLeft(group: AssessmentGroup) = if (group == null) null else group.id

}
