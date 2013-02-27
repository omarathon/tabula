package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, Command}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import reflect.BeanProperty
import scala.Some
import uk.ac.warwick.tabula.permissions.Permissions


class ListMarkerFeedbackCommand(val assignment:Assignment, module: Module, val user:CurrentUser,  val firstMarker:Boolean)
	extends Command[Seq[MarkerFeedbackItem]] with Unaudited with ReadOnly{

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Create, assignment)

	var userLookup = Wire.auto[UserLookupService]
	@BeanProperty var completedFeedback:Seq[MarkerFeedbackItem] = _

	def applyInternal():Seq[MarkerFeedbackItem] = {
		val submissions = assignment.getMarkersSubmissions(user.apparentUser)

		val markerFeedbacks = submissions.map { submission =>
			val student = userLookup.getUserByWarwickUniId(submission.universityId)
			val markerFeedback = assignment.getMarkerFeedback(submission.universityId, user.apparentUser)
			val firstMarkerFeedback =
				if (!firstMarker)
					assignment.feedbacks.find(_.universityId == submission.universityId) match {
						case Some(f) => f.firstMarkerFeedback
						case None => null
					}
				else null
			MarkerFeedbackItem(student, submission, markerFeedback.getOrElse(null), firstMarkerFeedback)
		}

		completedFeedback = markerFeedbacks.filter(_.markerFeedback.state == MarkingState.MarkingCompleted)
		markerFeedbacks.filterNot(_.markerFeedback.state == MarkingState.MarkingCompleted)

	}
}

case class MarkerFeedbackItem(student: User, submission: Submission, markerFeedback: MarkerFeedback, firstMarkerFeedback:MarkerFeedback)
