package uk.ac.warwick.courses.commands.feedback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.services.fileserver.RenderableZip
import uk.ac.warwick.courses.services.ZipService
import uk.ac.warwick.courses.data.model.Assignment

@Configurable
class AdminGetAllFeedbackCommand(assignment:Assignment) extends Command[RenderableZip] {
	@Autowired var zipService:ZipService =_
	
	override def apply = {
		val zip = zipService.getAllFeedbackZips(assignment)
		new RenderableZip(zip)
	}
	
	override def describe(d:Description) = d.assignment(assignment).properties(
			"feedbackCount" -> assignment.feedbacks.size
	)
}