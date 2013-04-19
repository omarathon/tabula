package uk.ac.warwick.tabula.coursework.commands.feedback

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{Description, ApplyWithCallback, ReadOnly, Command}
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.Permissions

class DownloadFirstMarkersFeedbackCommand(val module: Module, val assignment: Assignment, val currentUser:CurrentUser) extends Command[RenderableZip]
	with ReadOnly with ApplyWithCallback[RenderableZip] {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Create, assignment)

	var zipService = Wire[ZipService]

	protected def applyInternal() = {
		val markersSubs = assignment.getMarkersSubmissions(currentUser.apparentUser)
		val feedbacks = assignment.feedbacks.filter(f => markersSubs.exists(_.universityId == f.universityId))
		val firstMarkerFeedbacks = feedbacks.map(f => f.firstMarkerFeedback)
		val releasedFeedback = firstMarkerFeedbacks.filter(_.state == MarkingState.MarkingCompleted)
		val zip = zipService.getSomeMarkerFeedbacksZip(releasedFeedback)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

	// describe the thing that's happening.
	def describe(d: Description) {}
}