package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Submission, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, Command}


class ListMarkerFeedbackCommand(val assignment:Assignment, val user:CurrentUser,  val firstMarker:Boolean)
	extends Command[Seq[MarkerFeedbackItem]] with Unaudited with ReadOnly{


	def applyInternal:Seq[MarkerFeedbackItem] = {
		val submissions = assignment.getMarkersSubmissions(user.apparentUser).getOrElse(Seq())
		submissions.map { submission =>
			val parentFeedback = assignment.feedbacks.find(_.universityId == submission.universityId)
			val markerFeedback = parentFeedback.map(_.firstMarkerFeedback)
			MarkerFeedbackItem(submission, markerFeedback.getOrElse(null))
		}
	}

}

case class MarkerFeedbackItem(val submission: Submission, val markerFeedback: MarkerFeedback)
