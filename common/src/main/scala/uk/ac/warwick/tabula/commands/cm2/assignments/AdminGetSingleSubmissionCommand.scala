package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.AdminGetSingleSubmissionCommand._
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object AdminGetSingleSubmissionCommand {
	type Result = RenderableFile
	type Command = Appliable[Result] with AdminGetSingleSubmissionCommandState

	val AdminPermission = Permissions.Submission.Read

	def apply(assignment: Assignment, submission: Submission) =
		new AdminGetSingleSubmissionCommandInternal(assignment, submission)
			with ComposableCommand[Result]
			with AdminGetSingleSubmissionCommandPermissions
			with AdminGetSingleSubmissionCommandDescription
			with ReadOnly
			with AutowiringZipServiceComponent
}

trait AdminGetSingleSubmissionCommandState {
	def assignment: Assignment
	def submission: Submission
}

class AdminGetSingleSubmissionCommandInternal(val assignment: Assignment, val submission: Submission)
	extends CommandInternal[Result] with AdminGetSingleSubmissionCommandState {
	self: ZipServiceComponent =>

	override def applyInternal(): RenderableFile = zipService.getSubmissionZip(submission)
}

trait AdminGetSingleSubmissionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AdminGetSingleSubmissionCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		mustBeLinked(submission, assignment)
		p.PermissionCheck(AdminPermission, mandatory(submission))
	}
}

trait AdminGetSingleSubmissionCommandDescription extends Describable[Result] {
	self: AdminGetSingleSubmissionCommandState =>

	override lazy val eventName: String = "AdminGetSingleSubmission"

	override def describe(d: Description): Unit =
		d.submission(submission).properties(
			"studentId" -> submission.studentIdentifier,
			"attachmentCount" -> submission.allAttachments.size
		)
}