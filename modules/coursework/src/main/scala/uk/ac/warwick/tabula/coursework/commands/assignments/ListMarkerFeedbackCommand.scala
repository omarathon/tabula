package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Submission, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, Command}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService


class ListMarkerFeedbackCommand(val assignment:Assignment, val user:CurrentUser,  val firstMarker:Boolean)
	extends Command[Seq[MarkerFeedbackItem]] with Unaudited with ReadOnly{

	var userLookup = Wire.auto[UserLookupService]

	def applyInternal:Seq[MarkerFeedbackItem] = {
		val submissions = assignment.getMarkersSubmissions(user.apparentUser)
		submissions.map { submission =>
			val student = userLookup.getUserByWarwickUniId(submission.universityId)

			val parentFeedback = assignment.feedbacks.find(_.universityId == submission.universityId)
			val markerFeedback = parentFeedback.map(_.firstMarkerFeedback)

			MarkerFeedbackItem(student, submission, markerFeedback.getOrElse(null))
		}
	}
}

case class MarkerFeedbackItem(val student: User, val submission: Submission, val markerFeedback: MarkerFeedback)
