package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.system.TwoWayConverter

class AssignmentIdConverter extends TwoWayConverter[String, Assignment] {

	@Autowired var service: AssignmentService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getAssignmentById(_) }).orNull
	override def convertLeft(assignment: Assignment) = (Option(assignment) map {_.id}).orNull

}