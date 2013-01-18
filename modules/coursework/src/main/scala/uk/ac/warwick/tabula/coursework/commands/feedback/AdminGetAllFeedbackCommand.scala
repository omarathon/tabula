package uk.ac.warwick.tabula.coursework.commands.feedback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.data.model.Module


class AdminGetAllFeedbackCommand(module: Module, assignment: Assignment) extends Command[RenderableZip] with ReadOnly {
	
	mustBeLinked(assignment, module)
	PermissionsCheck(Participate(module))
	
	var zipService = Wire.auto[ZipService]

	override def applyInternal() = {
		val zip = zipService.getAllFeedbackZips(assignment)
		new RenderableZip(zip)
	}

	override def describe(d: Description) = d.assignment(assignment).properties(
		"feedbackCount" -> assignment.fullFeedback.size)
}