package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.Promises._

class UpstreamAssignmentIdConverter extends TwoWayConverter[String, UpstreamAssignment] {

	val service = promise { Wire[AssignmentMembershipService] }

	// Converter used for binding request
	override def convertRight(id: String) = service.get.getUpstreamAssignment(id).orNull
	
	// Formatter used for generating textual value in template
	override def convertLeft(assignment: UpstreamAssignment) = (Option(assignment) map { _.id }).orNull

}