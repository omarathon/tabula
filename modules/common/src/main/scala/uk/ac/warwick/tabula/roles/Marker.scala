package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.JavaImports

case class Marker(assignment: Assignment) extends BuiltInRole(MarkerRoleDefinition, assignment)

case object MarkerRoleDefinition extends UnassignableBuiltInRoleDefinition {
	
	override def description = "Marker"
	
	GrantsScopedPermission(
		Feedback.Create,
		Feedback.Read,
		Marks.Create,
		Marks.Read,
		Submission.Read
	)

}