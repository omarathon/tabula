package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._

/*
 * A role based on being the person who actually made the submission
 */
case class Submitter(submission: model.Submission) extends BuiltInRole {
	
	GrantsPermissionFor(submission, 
		Submission.Read(),
		Submission.SendReceipt()
	)

}