package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.{AssessmentService, ZipService}
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.commands.{ApplyWithCallback, ReadOnly, Command, Description}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.AssessmentMembershipService


/**
 * Downloads a feedback sheet per student in the assignment member list
 */
class DownloadFeedbackSheetsCommand(val module: Module, val assignment: Assignment) extends Command[RenderableZip]
	with ReadOnly with ApplyWithCallback[RenderableZip] with Logging {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)

	var members: Seq[User] = _

	var zipService = Wire.auto[ZipService]
	var assignmentService = Wire.auto[AssessmentService]
	var assignmentMembershipService = Wire.auto[AssessmentMembershipService]

	override def applyInternal():RenderableZip = {
		if (assignment.feedbackTemplate == null) logger.error("No feedback sheet for assignment - " + assignment.id)
		if (members == null)
			members = assignmentMembershipService.determineMembershipUsers(assignment)
		val zip = zipService.getMemberFeedbackTemplates(members, assignment)
		val renderable = new RenderableZip(zip)
		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) = {
		val members = assignmentMembershipService.determineMembershipUsers(assignment)
		d.assignment(assignment)
		d.studentIds(members.map(_.getWarwickId))
	}
}
