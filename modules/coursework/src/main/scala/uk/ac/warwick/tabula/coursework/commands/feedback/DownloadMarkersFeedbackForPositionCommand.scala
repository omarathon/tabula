package uk.ac.warwick.tabula.coursework.commands.feedback

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.fileserver.{RenderableFile, RenderableZip}
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipServiceComponent}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object DownloadMarkersFeedbackForPositionCommand {

	def apply(module: Module, assignment: Assignment, currentUser: CurrentUser, position: FeedbackPosition) =
		new DownloadMarkersFeedbackForPositionCommand(module, assignment, currentUser, position)
		with ComposableCommand[RenderableZip]
		with ApplyWithCallback[RenderableZip]
		with DownloadMarkersFeedbackForPositionDescription
		with DownloadMarkersFeedbackForPositionPermissions
		with DownloadMarkersFeedbackForPositionCommandState
		with ReadOnly
		with AutowiringZipServiceComponent
}

class DownloadMarkersFeedbackForPositionCommand(val module: Module, val assignment: Assignment, val currentUser:CurrentUser, val position: FeedbackPosition)
	extends CommandInternal[RenderableZip] with HasCallback[RenderableZip] {

	self: ZipServiceComponent =>

	override def applyInternal() = {
		val markersSubs = assignment.getMarkersSubmissions(currentUser.apparentUser)
		val feedbacks = assignment.feedbacks.filter(f => markersSubs.exists(_.universityId == f.universityId))
		val releasedMarkerFeedbacks = feedbacks.map(f => position match {
			case FirstFeedback => f.firstMarkerFeedback
			case SecondFeedback => f.secondMarkerFeedback
			case ThirdFeedback => f.thirdMarkerFeedback
		}).filter(_.state == MarkingState.MarkingCompleted)
		val zip = zipService.getSomeMarkerFeedbacksZip(releasedMarkerFeedbacks)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}
}

trait DownloadMarkersFeedbackForPositionDescription extends Describable[RenderableZip] {
	override def describe(d: Description) {}
}

trait DownloadMarkersFeedbackForPositionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DownloadMarkersFeedbackForPositionCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
	}
}

trait DownloadMarkersFeedbackForPositionCommandState {

	def module: Module
	def assignment: Assignment
	def currentUser: CurrentUser
	def position: FeedbackPosition

}