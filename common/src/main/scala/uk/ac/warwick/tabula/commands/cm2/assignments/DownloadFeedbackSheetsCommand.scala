package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.DownloadFeedbackSheetsCommand.Result
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

/**
 * Downloads a feedback sheet per student in the assignment member list
 */
object DownloadFeedbackSheetsCommand {
	type Result = RenderableFile
	type Command = Appliable[RenderableFile] with DownloadFeedbackSheetsCommandState

	def apply(assignment: Assignment): Command =
		new DownloadFeedbackSheetsCommandInternal(assignment)
			with ComposableCommand[Result]
			with DownloadFeedbackSheetsCommandPermissions
			with DownloadFeedbackSheetsCommandDescription
			with ReadOnly
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringZipServiceComponent
			with AllMembersDownloadFeedbackSheetsCommandAssignmentMembership

	def marker(assignment: Assignment, students: Seq[User]): Command =
		new DownloadFeedbackSheetsCommandInternal(assignment)
			with ComposableCommand[Result]
			with DownloadFeedbackSheetsCommandPermissions
			with DownloadFeedbackSheetsCommandDescription
			with ReadOnly
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringZipServiceComponent
			with DownloadFeedbackSheetsCommandAssignmentMembership {
			val members: Seq[User] = students
		}
}

trait DownloadFeedbackSheetsCommandState {
	def assignment: Assignment
}

trait DownloadFeedbackSheetsCommandAssignmentMembership {
	def members: Seq[User]
}

trait AllMembersDownloadFeedbackSheetsCommandAssignmentMembership extends DownloadFeedbackSheetsCommandAssignmentMembership {
	self: DownloadFeedbackSheetsCommandState with AssessmentMembershipServiceComponent =>

	lazy val members: Seq[User] = assessmentMembershipService.determineMembershipUsers(assignment)
}

class DownloadFeedbackSheetsCommandInternal(val assignment: Assignment)
	extends CommandInternal[Result] with DownloadFeedbackSheetsCommandState with Logging {
	self: ZipServiceComponent with DownloadFeedbackSheetsCommandAssignmentMembership =>

	override def applyInternal(): RenderableFile = {
		if (assignment.feedbackTemplate == null) logger.error("No feedback sheet for assignment - " + assignment.id)

		zipService.getMemberFeedbackTemplates(members, assignment)
	}

}

trait DownloadFeedbackSheetsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadFeedbackSheetsCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit =
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, mandatory(assignment))
}

trait DownloadFeedbackSheetsCommandDescription extends Describable[Result] {
	self: DownloadFeedbackSheetsCommandState
		with DownloadFeedbackSheetsCommandAssignmentMembership =>

	override lazy val eventName: String = "DownloadFeedbackSheets"

	override def describe(d: Description): Unit = {
		d.assignment(assignment)
		d.studentIds(members.flatMap(m => Option(m.getWarwickId)))
		d.studentUsercodes(members.map(_.getUserId))
	}
}