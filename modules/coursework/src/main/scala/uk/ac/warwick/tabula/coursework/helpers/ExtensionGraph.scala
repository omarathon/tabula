package uk.ac.warwick.tabula.coursework.helpers

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.forms.Extension

case class ExtensionGraph(
	universityId: String,
	user: User,
	isAwaitingReview: Boolean,
	hasApprovedExtension: Boolean,
	hasRejectedExtension: Boolean,
	duration: Int,
	requestedExtraDuration: Int,
	extension: Option[Extension])

object ExtensionGraph {
	def apply(extension: Extension) = new ExtensionGraph(
		extension.universityId,
		extension.getUserForUniversityId,
		extension.awaitingReview,
		extension.approved,
		extension.rejected,
		extension.duration,
		extension.requestedExtraDuration,
		Some(extension))
}
