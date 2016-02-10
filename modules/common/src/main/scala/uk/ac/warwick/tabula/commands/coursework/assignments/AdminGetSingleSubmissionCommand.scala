package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

class AdminGetSingleSubmissionCommand(val module: Module, val assignment: Assignment, val submission: Submission) extends Command[RenderableFile] with ReadOnly {
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read, submission)

	var zipService = Wire.auto[ZipService]

	override def applyInternal() = zipService.getSubmissionZip(submission)

	override def describe(d: Description) = d.submission(submission).properties(
		"studentId" -> submission.universityId,
		"attachmentCount" -> submission.allAttachments.size)
}
