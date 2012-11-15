package uk.ac.warwick.tabula.coursework.commands.feedback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.spring.Wire


class AdminGetAllFeedbackCommand(assignment: Assignment) extends Command[RenderableZip] with ReadOnly {
	var zipService = Wire.auto[ZipService]

	override def work = {
		val zip = zipService.getAllFeedbackZips(assignment)
		new RenderableZip(zip)
	}

	override def describe(d: Description) = d.assignment(assignment).properties(
		"feedbackCount" -> assignment.feedbacks.size)
}