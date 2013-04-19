package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.Promises._

class AssignmentIdConverter extends TwoWayConverter[String, Assignment] {

	val service = promise { Wire[AssignmentService] }

	override def convertRight(id: String) = (Option(id) flatMap { service.get.getAssignmentById(_) }).orNull
	override def convertLeft(assignment: Assignment) = (Option(assignment) map {_.id}).orNull

}