package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data.model.Assignment

case class Marker(assignment: Assignment) extends BuiltInRole(MarkerRoleDefinition, assignment)

case object MarkerRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Marker"
	
	GrantsScopedPermission(
		Feedback.Create,
		Feedback.Read,
		Marks.Create,
		Marks.Read,
		Submission.Read
	)

}