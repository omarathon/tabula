package uk.ac.warwick.tabula.profiles.commands.tutor

import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.StudentRelationship

trait DepartmentCommand {
	val department: Department
	val apparentUser: User
}

trait NotifiesAffectedStudents extends Notifies[Seq[Option[StudentRelationship]], StudentRelationship] {
	this: DepartmentCommand =>

	def emit(newRelationships: Seq[Option[StudentRelationship]]): Seq[Notification[StudentRelationship]] = {
		Nil // TODO
	}
}
