package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking, PermissionsCheckingMethods}


/**
 * Download one or more submissions from an assignment, as a Zip, for you as a marker.
 */
object DownloadMarkersSubmissionsCommand {
	def apply(module: Module, assignment: Assignment, user: CurrentUser) =
		new DownloadMarkersSubmissionsCommand(module, assignment, user)
		with ComposableCommand[RenderableZip]
		with AutowiringZipServiceComponent
		with AutowiringAssignmentServiceComponent
		with AutowiringStateServiceComponent
		with DownloadMarkersSubmissionsDescription
		with DownloadMarkersSubmissionsCommandState
		with DownloadMarkersSubmissionsPermissions
		with ReadOnly
}

class DownloadMarkersSubmissionsCommand(val module: Module, val assignment: Assignment, val user: CurrentUser)
		extends CommandInternal[RenderableZip] with ApplyWithCallback[RenderableZip]
		with DownloadMarkersSubmissionsDescription with DownloadMarkersSubmissionsCommandState {

	self: ZipServiceComponent with AssignmentServiceComponent with StateServiceComponent =>

	override def applyInternal(): RenderableZip = {
		val submissions = assignment.getMarkersSubmissions(user.apparentUser)
		
		if (submissions.isEmpty) throw new ItemNotFoundException

		// do not download submissions where the marker has completed marking
		val filteredSubmissions = submissions.filter{ submission =>
			val markerFeedback = assignment.getMarkerFeedbackForCurrentPosition(submission.universityId, user.apparentUser)
			markerFeedback.exists(mf => mf.state != MarkingCompleted)
		}

		// update the state to downloaded for any marker feedback that exists.
		filteredSubmissions.foreach( s =>
			assignment.feedbacks
			.find(f => f.universityId == s.universityId)
			.map(f => f.firstMarkerFeedback)
			.foreach(mf =>
				if(mf != null && mf.state == ReleasedForMarking)
					stateService.updateState(mf, InProgress)
			)
		)

		val zip = zipService.getSomeSubmissionsZip(filteredSubmissions)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

}

trait DownloadMarkersSubmissionsDescription extends Describable[RenderableZip] {

	self: DownloadMarkersSubmissionsCommandState =>

	override lazy val eventName = "DownloadMarkersSubmissions"

	override def describe(d: Description) {
		val downloads = assignment.getMarkersSubmissions(user.apparentUser)

		d.assignment(assignment)
			.submissions(downloads)
			.studentIds(downloads.map(_.universityId))
			.properties("submissionCount" -> downloads.size)
	}

}

trait DownloadMarkersSubmissionsPermissions extends PermissionsCheckingMethods with RequiresPermissionsChecking {

	self: DownloadMarkersSubmissionsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Submission.Read, assignment)
	}

}

trait DownloadMarkersSubmissionsCommandState {
	def module: Module
	def assignment: Assignment
	def user: CurrentUser
}