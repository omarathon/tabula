package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback, Submission}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._


/**
 * Download one or more submissions from an assignment, as a Zip, for you as a marker.
 */
object DownloadMarkersSubmissionsCommand {
	def apply(assignment: Assignment, marker: User, submitter: CurrentUser) =
		new DownloadMarkersSubmissionsCommand(assignment, marker, submitter)
			with ComposableCommand[RenderableFile]
			with AutowiringZipServiceComponent
			with AutowiringAssessmentServiceComponent
			with AutowiringStateServiceComponent
			with DownloadMarkersSubmissionsDescription
			with DownloadMarkersSubmissionsCommandState
			with DownloadMarkersSubmissionsPermissions
			with ReadOnly
}

class DownloadMarkersSubmissionsCommand(val assignment: Assignment, val marker: User, val submitter: CurrentUser)
	extends CommandInternal[RenderableFile] with DownloadMarkersSubmissionsCommandState with CanProxy {

	self: ZipServiceComponent with AssessmentServiceComponent with StateServiceComponent =>

	override def applyInternal(): RenderableFile = {
		zipService.getSomeSubmissionsZip(submissions)
	}
}

trait DownloadMarkersSubmissionsDescription extends Describable[RenderableFile] {

	self: DownloadMarkersSubmissionsCommandState =>

	override lazy val eventName = "DownloadMarkersSubmissions"

	override def describe(d: Description) {
		d.assignment(assignment)
			.submissions(submissions)
			.studentIds(submissions.flatMap(_.universityId))
			.studentUsercodes(submissions.map(_.usercode))
			.properties("submissionCount" -> submissions.size)
	}

}

trait DownloadMarkersSubmissionsPermissions extends PermissionsCheckingMethods with RequiresPermissionsChecking {

	self: DownloadMarkersSubmissionsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Submission.Read, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}

}

trait DownloadMarkersSubmissionsCommandState {
	def assignment: Assignment
	def marker: User
	def submitter: CurrentUser
	var markerFeedback: JList[MarkerFeedback] = JArrayList()
	def students: Seq[User] = markerFeedback.asScala.map(_.student)
	// can only download these students submissions or a subset
	def markersStudents: Seq[User] = assignment.cm2MarkerAllocations(marker).flatMap(_.students).distinct

	lazy val submissions: Seq[Submission] = {
		// filter out ones we aren't the marker for
		val studentsToDownload = students.filter(markersStudents.contains).map(_.getUserId)
		assignment.submissions.asScala.filter(s => studentsToDownload.contains(s.usercode))
	}
}