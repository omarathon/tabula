package uk.ac.warwick.tabula.coursework.commands.assignments

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment

class AdminGetSingleSubmissionCommand(val module: Module, val assignment: Assignment, val submission: Submission) extends Command[RenderableZip] with ReadOnly {
	mustBeLinked(assignment, module)
	PermissionCheck(Permission.Submission.Read(), submission)
	
	var zipService = Wire.auto[ZipService]

	override def applyInternal() = {
		val zip = zipService.getSubmissionZip(submission)
		new RenderableZip(zip)
	}

	override def describe(d: Description) = d.submission(submission).properties(
		"studentId" -> submission.universityId,
		"attachmentCount" -> submission.allAttachments.size)
}
