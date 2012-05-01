package uk.ac.warwick.courses.commands.feedback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.services.fileserver.RenderableZip
import uk.ac.warwick.courses.services.ZipService
import uk.ac.warwick.courses.commands.ReadOnly

@Configurable
class AdminGetSingleFeedbackCommand(feedback:Feedback) extends Command[RenderableZip] with ReadOnly {
	@Autowired var zipService:ZipService =_
	
	override def apply = {
		val zip = zipService.getFeedbackZip(feedback)
		new RenderableZip(zip)
	}
	
	override def describe(d:Description) = d.feedback(feedback).properties(
			"studentId" -> feedback.universityId,
			"attachmentCount" -> feedback.attachments.size
	)
}