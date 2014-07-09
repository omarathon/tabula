package uk.ac.warwick.tabula.coursework.helpers

import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.forms.Extension

case class ExtensionGraph(
	universityId: String,
	user: User,
	deadline: DateTime,
	isAwaitingReview: Boolean,
	hasApprovedExtension: Boolean,
	hasRejectedExtension: Boolean,
	duration: Int,
	requestedExtraDuration: Int,
	extension: Option[Extension])

object ExtensionGraph {
	def apply(extension: Extension, user: User) = new ExtensionGraph(
		extension.universityId,
		user,
		extension.assignment.submissionDeadline(user),
		extension.awaitingReview,
		extension.approved,
		extension.rejected,
		extension.duration,
		extension.requestedExtraDuration,
		Some(extension))
}
