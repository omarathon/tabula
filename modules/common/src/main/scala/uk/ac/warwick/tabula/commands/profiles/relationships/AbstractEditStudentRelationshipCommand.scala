package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Notifies}
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationship}
import uk.ac.warwick.tabula.services.RelationshipService


abstract class AbstractEditStudentRelationshipCommand extends Command[Seq[StudentRelationship]]  with Notifies[Seq[StudentRelationship], StudentRelationship] {

	var relationshipService: RelationshipService = Wire[RelationshipService]

	var notifyStudent: Boolean = false
	var notifyOldAgents: Boolean = false
	var notifyNewAgent: Boolean = false

	def applyInternal(): Seq[StudentRelationship]

	def oldAgents: Seq[Member]
}
