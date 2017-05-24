package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands.{Notifies, UserAware}
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback, Notification}
import uk.ac.warwick.tabula.data.model.notifications.cm2.ReleaseToMarkerNotification
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._

trait FeedbackReleasedNotifier[A] extends Notifies[A, Seq[MarkerFeedback]] {

	this: ReleasedState with UserAware with Logging =>

	def blankNotification: ReleaseToMarkerNotification

	def emit(commandResult: A): Seq[Notification[MarkerFeedback, Assignment]] = {
		// emit notifications to each marker that has new feedback
		val markerMap : Map[String, Seq[MarkerFeedback]] = newReleasedFeedback.asScala.groupBy(_.marker.getUserId)

		markerMap.map{ case (usercode, markerFeedback) =>
			val notification = Notification.init(blankNotification, user, markerFeedback, assignment)
			notification.recipientUserId = usercode
			notification
		}.toSeq
	}
}

trait ReleasedState {
	val assignment: Assignment
	var newReleasedFeedback: JList[MarkerFeedback] = JArrayList()
}
