package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._

/*
 * Allow students to receive their own feedback
 */
case class FeedbackRecipient(feedback: model.Feedback) extends BuiltInRole(feedback, FeedbackRecipientRoleDefinition) 

case object FeedbackRecipientRoleDefinition extends BuiltInRoleDefinition {
	
	GrantsScopedPermission( 
		Feedback.Read,
		Feedback.Rate
	)

}
