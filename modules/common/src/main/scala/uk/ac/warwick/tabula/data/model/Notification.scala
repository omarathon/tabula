package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.userlookup.User

/**
 * Notifications have a similar structure to Open Social Activities
 * One of the common things we will want to do with notifications is
 * feed them into Open Social activity streams.
 *
 * A notification could be generated when a student submits an assignment
 * in this case ....
 * actor = the student submitting the assignment
 * verb = submit
 * _object = the submission
 * target = the assignment that we are submitting to
 * title = "Submission made"
 * content = "X made a submission to assignment Y on {date_time}"
 * url = "/path/to/assignment/with/this/submission/highlighted"
 * recipients = who is interested in this notification - activity streams won't
 * need this information
 */
trait Notification[A]{
	val actor: User
	val verb: String
	val _object: A
	val target: Option[AnyRef]

	def title: String
	def content: String
	def url: String
	def recipients: Seq[User]
}
