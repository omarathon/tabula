package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permission._
import uk.ac.warwick.tabula.data.model.Assignment

case class Marker(assignment: Assignment) extends BuiltInRole {
	
	GrantsPermissionFor(assignment,
		Feedback.Create(),
		Submission.Read()
	)

}