package uk.ac.warwick.tabula.data.model.notifications.cm2

import uk.ac.warwick.tabula.data.model.{Assignment, FirstMarkersMap, SecondMarkersMap}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class ReleaseToMarkerNotificationHelper(assignment: Assignment) {

	val allMarkers: Seq[User] = assignment.cm2MarkerAllocations.map(_.marker)

	val firstMarkerMaps: Seq[FirstMarkersMap] = assignment.firstMarkers.asScala

	val secondMarkerMaps: Seq[SecondMarkersMap] = assignment.secondMarkers.asScala

	def firstMarkers: Seq[User] = allMarkers.filter(markerUser => firstMarkerMaps.map(_.marker_id).contains(markerUser.getUserId))

	def secondMarkers: Seq[User] = allMarkers.filter(markerUser => secondMarkerMaps.map(_.marker_id).contains(markerUser.getUserId))

}
