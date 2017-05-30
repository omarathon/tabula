package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.assignments.DownloadSubmissionsCommand._
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.jobs.zips.SubmissionZipFileJob
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.jobs.{AutowiringJobServiceComponent, JobInstance, JobServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

/**
 * Download one or more submissions from an assignment, as a Zip.
 */
object DownloadSubmissionsCommand {
	case class Result(
		submissions: Seq[Submission],
		output: Either[RenderableFile, JobInstance]
	)

	type Command = Appliable[Result] with DownloadSubmissionsCommandRequest

	def apply(assignment: Assignment, user: CurrentUser): Command =
		new DownloadSubmissionsCommandInternal(assignment, user)
			with ComposableCommand[Result]
			with DownloadSubmissionsCommandPermissions
			with DownloadSubmissionsCommandDescription
			with ReadOnly
			with AutowiringZipServiceComponent
			with AutowiringSubmissionServiceComponent
			with AutowiringJobServiceComponent
}

trait DownloadSubmissionsCommandState extends SelectedStudentsState {
	def assignment: Assignment
	def user: CurrentUser
}

trait DownloadSubmissionsCommandRequest extends SelectedStudentsRequest {
	self: DownloadSubmissionsCommandState =>

	var filename: String = _
}

class DownloadSubmissionsCommandInternal(val assignment: Assignment, val user: CurrentUser)
	extends CommandInternal[Result] with DownloadSubmissionsCommandState with DownloadSubmissionsCommandRequest {
	self: ZipServiceComponent
		with SubmissionServiceComponent
		with JobServiceComponent =>

	override def applyInternal(): Result = {
		if (submissions.exists(_.assignment != assignment)) {
			throw new IllegalStateException("Submissions don't match the assignment")
		}

		val output =
			if (submissions.size < SubmissionZipFileJob.minimumSubmissions) {
				val zip = zipService.getSomeSubmissionsZip(submissions)
				Left(zip)
			} else {
				Right(jobService.add(Option(user), SubmissionZipFileJob(submissions.map(_.id))))
			}

		Result(submissions, output)
	}

}

trait DownloadSubmissionsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadSubmissionsCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit =
		p.PermissionCheck(Permissions.Submission.Read, mandatory(assignment))
}

trait DownloadSubmissionsCommandDescription extends Describable[Result] {
	self: DownloadSubmissionsCommandState =>

	override lazy val eventName: String = "DownloadSubmissions"

	override def describe(d: Description): Unit = d.assignment(assignment)

	override def describeResult(d: Description, result: Result): Unit =
		d.submissions(result.submissions)
			.studentIds(result.submissions.flatMap(_.universityId))
			.studentUsercodes(result.submissions.map(_.usercode))
			.properties("submissionCount" -> result.submissions.size)
}