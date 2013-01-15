package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.data.model.{MarkingCompleted, MarkerFeedback, Submission, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, Command}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import reflect.BeanProperty


class ListMarkerFeedbackCommand(val assignment:Assignment, val user:CurrentUser,  val firstMarker:Boolean)
	extends Command[Seq[MarkerFeedbackItem]] with Unaudited with ReadOnly{

	var userLookup = Wire.auto[UserLookupService]
	@BeanProperty var completedFeedback:Seq[MarkerFeedbackItem] = _

	def applyInternal():Seq[MarkerFeedbackItem] = {
		val submissions = assignment.getMarkersSubmissions(user.apparentUser)

		val markerFeedbacks = submissions.map { submission =>
			val student = userLookup.getUserByWarwickUniId(submission.universityId)
			val markerFeedback = assignment.getMarkerFeedback(submission.universityId, user.apparentUser)
			MarkerFeedbackItem(student, submission, markerFeedback.getOrElse(null))
		}

		completedFeedback = markerFeedbacks.filter(_.markerFeedback.state == MarkingCompleted)
		markerFeedbacks.filterNot(_.markerFeedback.state == MarkingCompleted)

	}
}

case class MarkerFeedbackItem(student: User, submission: Submission, markerFeedback: MarkerFeedback)
