package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._

case class AssignmentSubmitter(assignment: model.Assignment) extends BuiltInRole(assignment, AssignmentSubmitterRoleDefinition)

case object AssignmentSubmitterRoleDefinition extends BuiltInRoleDefinition {
	GrantsScopedPermission(
		Submission.Create,
		Extension.MakeRequest
	)
}