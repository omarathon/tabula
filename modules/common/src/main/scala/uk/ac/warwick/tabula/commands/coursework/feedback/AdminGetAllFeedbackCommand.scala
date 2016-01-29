package uk.ac.warwick.tabula.commands.coursework.feedback
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile


class AdminGetAllFeedbackCommand(module: Module, assignment: Assignment) extends Command[RenderableFile] with ReadOnly {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)

	var zipService = Wire.auto[ZipService]

	override def applyInternal() = zipService.getAllFeedbackZips(assignment)

	override def describe(d: Description) = d.assignment(assignment).properties(
		"feedbackCount" -> assignment.fullFeedback.size)
}