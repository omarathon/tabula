package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{RelationshipType, Member, StudentRelationship}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.RelationshipType.{Supervisor, PersonalTutor}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking, PerformsPermissionsChecking}
import uk.ac.warwick.tabula.commands._

// Don't need this, unless there is specific state on the command which the controller needs access to.
//
//trait ViewRelatedStudentsCommand extends ComposableCommand[Seq[StudentRelationship]]  {
//	this:ViewRelatedStudentsCommandInternal=>
//}
object ViewRelatedStudentsCommand{
	def apply(currentMember:Member,relationshipType:RelationshipType):Command[Seq[StudentRelationship]] = {
		  new ViewRelatedStudentsCommandInternal(currentMember,relationshipType)
				with ComposableCommand[Seq[StudentRelationship]]
				with AutowiringRelationshipServiceComponent
        with Unaudited
	}
}

class ViewRelatedStudentsCommandInternal(val currentMember: Member, val relationshipType:RelationshipType)
	extends CommandInternal[Seq[StudentRelationship]] with RequiresPermissionsChecking {

	this: RelationshipServiceComponent =>

	def permissionsCheck(p:PermissionsChecking){
		relationshipType match {
			case PersonalTutor=>p.PermissionCheck(Permissions.Profiles.Read.PersonalTutees, currentMember)
			case Supervisor=>	p.PermissionCheck(Permissions.Profiles.Read.Supervisees, currentMember)
		}

	}

	def applyInternal(): Seq[StudentRelationship] = transactional(readOnly = true) {
		relationshipService.listStudentRelationshipsWithMember(relationshipType, currentMember)
	}

}
