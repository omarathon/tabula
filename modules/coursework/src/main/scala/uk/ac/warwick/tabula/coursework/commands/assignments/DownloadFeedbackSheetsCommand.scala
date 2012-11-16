package uk.ac.warwick.tabula.coursework.commands.assignments

import org.springframework.beans.factory.annotation.{Autowired, Configurable}
import reflect.BeanProperty
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.{AssignmentService, ZipService}
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.commands.{ApplyWithCallback, ReadOnly, Command, Description}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire


/**
 * Downloads a feedback sheet per student in the assignment member list
 */
class DownloadFeedbackSheetsCommand extends Command[RenderableZip]
	with ReadOnly with ApplyWithCallback[RenderableZip] with Logging {

	@BeanProperty var assignment: Assignment = _
	var zipService = Wire.auto[ZipService]
	var assignmentService = Wire.auto[AssignmentService]

	override def work():RenderableZip = {
		if (assignment.feedbackTemplate == null) logger.error("No feedback sheet for assignment - "+assignment.id)
		val members = assignmentService.determineMembershipUsers(assignment)
		val zip = zipService.getMemberFeedbackTemplates(members, assignment)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) = {
		val members = assignmentService.determineMembershipUsers(assignment)
		d.assignment(assignment)
		d.studentIds(members.map(_.getWarwickId))
	}
}
