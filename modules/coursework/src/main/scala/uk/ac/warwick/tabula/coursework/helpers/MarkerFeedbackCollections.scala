package uk.ac.warwick.tabula.coursework.helpers

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.MarkerFeedbackItem
import uk.ac.warwick.tabula.data.model.{MarkingState, Module, Assignment}
import uk.ac.warwick.tabula.services.UserLookupService

case class MarkerFeedbackCollections(
	inProgressFeedback: Seq[MarkerFeedbackItem],
	completedFeedback: Seq[MarkerFeedbackItem],
	rejectedFeedback: Seq[MarkerFeedbackItem]
)

trait MarkerFeedbackCollecting  {
	def getMarkerFeedbackCollections(assignment: Assignment, module: Module, user: CurrentUser, userLookup: UserLookupService) = {
		val submissions = assignment.getMarkersSubmissions(user.apparentUser)

		val (inProgressFeedback: Seq[MarkerFeedbackItem], completedFeedback: Seq[MarkerFeedbackItem], rejectedFeedback: Seq[MarkerFeedbackItem]) = {
			val markerFeedbackItems = submissions.map{ submission =>
				val student = userLookup.getUserByWarwickUniId(submission.universityId)
				val feedbacks = assignment.getAllMarkerFeedbacks(submission.universityId, user.apparentUser).reverse
				MarkerFeedbackItem(student, submission, feedbacks)
			}.filterNot(_.feedbacks.isEmpty)

			(
				markerFeedbackItems.filter(f => f.feedbacks.last.state != MarkingState.MarkingCompleted && f.feedbacks.last.state != MarkingState.Rejected),
				markerFeedbackItems.filter(f => f.feedbacks.last.state == MarkingState.MarkingCompleted),
				markerFeedbackItems.filter(f => f.feedbacks.last.state == MarkingState.Rejected)
				)
		}

		MarkerFeedbackCollections(inProgressFeedback, completedFeedback, rejectedFeedback)
	}

}
