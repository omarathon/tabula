package uk.ac.warwick.courses.commands.assignments

import org.springframework.beans.factory.annotation.{Autowired, Configurable}
import reflect.BeanProperty
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.services.{AssignmentService, ZipService}
import uk.ac.warwick.courses.services.fileserver.RenderableZip
import uk.ac.warwick.courses.commands.{ApplyWithCallback, ReadOnly, Command, Description}
import uk.ac.warwick.courses.helpers.Logging

/**
 * Downloads a feedback sheet per student in the assignment member list
 */
@Configurable
class DownloadFeedbackSheetsCommand extends Command[RenderableZip]
	with ReadOnly with ApplyWithCallback[RenderableZip] with Logging {

	@BeanProperty var assignment: Assignment = _
	@Autowired var zipService: ZipService = _
	@Autowired var assignmentService:AssignmentService =_

	override def apply():RenderableZip = {
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
		.studentIds(members.map(_.getWarwickId))
	}
}
