package uk.ac.warwick.tabula.coursework.commands.feedback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Feedback}
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.services.ZipService


class AdminGetSingleFeedbackCommand(module: Module, assignment: Assignment, feedback: Feedback) extends Command[RenderableZip] with ReadOnly {
	mustBeLinked(assignment, module)
	PermissionsCheck(Participate(module))
	
	var zipService = Wire.auto[ZipService]

	override def applyInternal() = {
		val zip = zipService.getFeedbackZip(feedback)
		new RenderableZip(zip)
	}

	override def describe(d: Description) = d.feedback(feedback).properties(
		"studentId" -> feedback.universityId,
		"attachmentCount" -> feedback.attachments.size)
}

class AdminGetSingleMarkerFeedbackCommand(markerFeedback: MarkerFeedback) extends Command[RenderableZip] with ReadOnly {
	var zipService = Wire.auto[ZipService]

	override def applyInternal() = {
		val zip = zipService.getSomeMarkerFeedbacksZip(Seq(markerFeedback))
		new RenderableZip(zip)
	}

	override def describe(d: Description) = d.feedback(markerFeedback.feedback).properties(
		"studentId" -> markerFeedback.feedback.universityId,
		"attachmentCount" -> markerFeedback.attachments.size)
}