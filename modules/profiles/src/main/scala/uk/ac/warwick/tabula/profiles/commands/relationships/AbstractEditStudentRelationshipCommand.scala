package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.commands.{Command,  Notifies}
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationship}
import uk.ac.warwick.tabula.services.RelationshipService


abstract class AbstractEditStudentRelationshipCommand extends Command[Seq[StudentRelationship]]  with Notifies[Seq[StudentRelationship], StudentRelationship] {

	var relationshipService: RelationshipService = _ // let this be set by the calling command to facilitate testing

	var notifyStudent: Boolean = false
	var notifyOldAgent: Boolean = false
	var notifyNewAgent: Boolean = false

	def oldAgent: Option[Member]

	def applyInternal(): Seq[StudentRelationship]

}
