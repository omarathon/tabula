package uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications

import uk.ac.warwick.tabula.commands.{UserAware, Notifies}
import uk.ac.warwick.tabula.data.model.{Notification, Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer

trait FeedbackReleasedNotifier[A] extends Notifies[A, Seq[MarkerFeedback]] {

	this: ReleasedState with UserAware with UserLookupComponent with Logging =>

	def isFirstMarker: Boolean

	def emit(commandResult: A): Seq[Notification[Seq[MarkerFeedback]]] = {

		// emit notifications to each second marker that has new feedback
		val markerMap : Map[String, Seq[MarkerFeedback]] = newReleasedFeedback.asScala.groupBy(mf => {
			val marker = mf.getMarkerUsercode
			marker.getOrElse("unassigned")
		})

		val unassignedFeedback = markerMap.get("unassigned")
		if(unassignedFeedback.isDefined){
			logger.warn(s"${unassignedFeedback.get.size} marker feedback were released without a second marker for ${assignment.name}")
		}

		markerMap.filterNot(_._1 == "unassigned").map{ case (usercode, markerFeedbacks) =>
			val recepient = userLookup.getUserByUserId(usercode)
			new ReleaseToMarkerNotification(user, recepient, markerFeedbacks, assignment, isFirstMarker) with FreemarkerTextRenderer
		}.toSeq
	}
}

trait ReleasedState {
	val assignment: Assignment
	var newReleasedFeedback: JList[MarkerFeedback] = JArrayList()
}
