package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands.{Notifies, UserAware}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, MarkerFeedback, Notification}
import uk.ac.warwick.tabula.data.model.notifications.cm2.ReleaseToMarkerNotification
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Tap._

import scala.collection.JavaConverters._

trait FeedbackReleasedNotifier extends Notifies[Seq[Feedback], Seq[MarkerFeedback]] {

	self: ReleasedState with UserAware with Logging =>

	def emit(commandResult: Seq[Feedback]): Seq[Notification[MarkerFeedback, Assignment]] = {
		// emit notifications to each marker that has new feedback
		val markerMap : Map[String, Seq[MarkerFeedback]] = newReleasedFeedback.asScala.groupBy(_.marker.getUserId)

		markerMap.flatMap{
			case (usercode, markerFeedback) if markerFeedback.nonEmpty && usercode != null =>
				Some(Notification.init(new ReleaseToMarkerNotification, user, markerFeedback, assignment).tap{n =>
					n.recipientUserId = usercode
				})
			case _ => None
		}.toSeq

	}
}

trait ReleasedState {
	val assignment: Assignment
	var newReleasedFeedback: JList[MarkerFeedback] = JArrayList()
}
