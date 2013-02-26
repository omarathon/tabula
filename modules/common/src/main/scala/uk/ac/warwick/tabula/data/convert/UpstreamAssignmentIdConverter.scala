package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.system.TwoWayConverter

class UpstreamAssignmentIdConverter extends TwoWayConverter[String, UpstreamAssignment] {

	@Autowired var service: AssignmentService = _

	// Converter used for binding request
	override def convertRight(id: String) = service.getUpstreamAssignment(id).orNull
	
	// Formatter used for generating textual value in template
	override def convertLeft(assignment: UpstreamAssignment) = Option(assignment) map { _.id } orNull

}