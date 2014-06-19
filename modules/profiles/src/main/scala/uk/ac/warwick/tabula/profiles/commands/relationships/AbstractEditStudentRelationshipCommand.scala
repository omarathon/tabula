package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{SelfValidating, Command,  Notifies}
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationship}
import uk.ac.warwick.tabula.services.RelationshipService


abstract class AbstractEditStudentRelationshipCommand extends Command[Seq[StudentRelationship]]  with Notifies[Seq[StudentRelationship], StudentRelationship] {

	var relationshipService = Wire[RelationshipService]

	var notifyStudent: Boolean = false
	var notifyOldAgent: Boolean = false
	var notifyNewAgent: Boolean = false

	def oldAgent: Option[Member]

}
