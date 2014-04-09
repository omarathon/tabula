package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AssignmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.AssignmentServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssignmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssignmentServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.coursework.web.controllers.StudentCourseworkCommand.StudentAssignments
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.web.controllers.{StudentCourseworkCommandInternal, StudentCourseworkCommandHelper}

object StudentCourseworkFullScreenCommand {
	def apply(memberOrUser: MemberOrUser): Appliable[StudentAssignments] =
		new StudentCourseworkFullScreenCommandInternal(memberOrUser)
			with ComposableCommand[StudentAssignments]
			with StudentCourseworkFullScreenCommandPermissions
			with AutowiringAssignmentServiceComponent
			with AutowiringAssignmentMembershipServiceComponent
			with AutowiringFeaturesComponent
			with StudentCourseworkCommandHelper
			with ReadOnly with Unaudited
}

class StudentCourseworkFullScreenCommandInternal(val memberOrUser: MemberOrUser) extends StudentCourseworkCommandInternal
	with StudentCourseworkFullScreenCommandState {

	self: AssignmentServiceComponent with
		  AssignmentMembershipServiceComponent with
		  FeaturesComponent with
			StudentCourseworkCommandHelper =>

	override def overridableAssignmentsWithFeedback = assignmentService.getAssignmentsWithFeedback(memberOrUser.universityId)

	override def overridableEnrolledAssignments = assignmentMembershipService.getEnrolledAssignments(memberOrUser.asUser)

	override def overridableAssignmentsWithSubmission = assignmentService.getAssignmentsWithSubmission(memberOrUser.universityId)

	override val universityId: String = memberOrUser.universityId

	override val user: User = memberOrUser.asUser

}

trait StudentCourseworkFullScreenCommandState {
	def memberOrUser: MemberOrUser
}

trait StudentCourseworkFullScreenCommandPermissions extends RequiresPermissionsChecking {
	self: StudentCourseworkFullScreenCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		memberOrUser.asMember.foreach { member =>
			p.PermissionCheck(Permissions.Profiles.Read.Coursework, member)
			p.PermissionCheck(Permissions.Submission.Read, member)
			p.PermissionCheck(Permissions.Feedback.Read, member)
			p.PermissionCheck(Permissions.Extension.Read, member)
		}
	}
}
