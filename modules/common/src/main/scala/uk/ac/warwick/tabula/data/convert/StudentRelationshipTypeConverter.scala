package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.system.TwoWayConverter

class StudentRelationshipTypeConverter extends TwoWayConverter[String, StudentRelationshipType] {

	@Autowired var service: RelationshipService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getStudentRelationshipTypeById(_) }).orNull
	override def convertLeft(relationshipType: StudentRelationshipType) = (Option(relationshipType) map {_.id}).orNull
	
}