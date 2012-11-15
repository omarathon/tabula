package uk.ac.warwick.courses.commands.assignments
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.services.fileserver.RenderableZip
import uk.ac.warwick.courses.services.ZipService
import uk.ac.warwick.courses.commands.ReadOnly

@Configurable
class AdminGetSingleSubmissionCommand(submission: Submission) extends Command[RenderableZip] with ReadOnly {
	@Autowired var zipService: ZipService = _

	override def apply = {
		val zip = zipService.getSubmissionZip(submission)
		new RenderableZip(zip)
	}

	override def describe(d: Description) = d.submission(submission).properties(
		"studentId" -> submission.universityId,
		"attachmentCount" -> submission.allAttachments.size)
}
