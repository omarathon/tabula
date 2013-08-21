package uk.ac.warwick.tabula.profiles.commands.tutor

import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.RelationshipService

trait DepartmentCommand {
	val department: Department
	val apparentUser: User
	
	var service: RelationshipService
}

trait NotifiesAffectedStudents extends Notifies[Seq[StudentRelationship], StudentRelationship] {
	this: DepartmentCommand =>
		
	var notifyTutee: Boolean = false
	var notifyOldTutor: Boolean = false
	var notifyNewTutor: Boolean = false

	def emit(modifiedRelationships: Seq[StudentRelationship]): Seq[Notification[StudentRelationship]] = {
		if (notifyTutee) {
			
		}
		
		if (notifyOldTutor) {
			
		}
		
		if (notifyNewTutor) {
			
		}
		
		modifiedRelationships
			.groupBy(_.agent) // group into map by tutor id
		
		Nil // TODO
	}
}
