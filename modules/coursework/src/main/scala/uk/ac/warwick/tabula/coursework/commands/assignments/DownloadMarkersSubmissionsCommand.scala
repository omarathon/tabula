package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking, PermissionsCheckingMethods}


/**
 * Download one or more submissions from an assignment, as a Zip, for you as a marker.
 */
object DownloadMarkersSubmissionsCommand {
	def apply(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser) =
		new DownloadMarkersSubmissionsCommand(module, assignment, marker, submitter)
		with ComposableCommand[RenderableZip]
		with ApplyWithCallback[RenderableZip]
		with AutowiringZipServiceComponent
		with AutowiringAssessmentServiceComponent
		with AutowiringStateServiceComponent
		with DownloadMarkersSubmissionsDescription
		with DownloadMarkersSubmissionsCommandState
		with DownloadMarkersSubmissionsPermissions
		with ReadOnly
}

class DownloadMarkersSubmissionsCommand(val module: Module, val assignment: Assignment, val marker: User, val submitter: CurrentUser)
	extends CommandInternal[RenderableZip] with HasCallback[RenderableZip] with CanProxy {

	self: ZipServiceComponent with AssessmentServiceComponent with StateServiceComponent =>

	override def applyInternal(): RenderableZip = {
		val submissions = assignment.getMarkersSubmissions(marker)
		
		if (submissions.isEmpty) throw new ItemNotFoundException

		// do not download submissions where the marker has completed marking
		val filteredSubmissions = submissions.filter{ submission =>
			val markerFeedback = assignment.getMarkerFeedbackForCurrentPosition(submission.universityId, marker)
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
		val downloads = assignment.getMarkersSubmissions(marker)

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
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}

}

trait DownloadMarkersSubmissionsCommandState {
	def module: Module
	def assignment: Assignment
	def marker: User
	def submitter: CurrentUser
}