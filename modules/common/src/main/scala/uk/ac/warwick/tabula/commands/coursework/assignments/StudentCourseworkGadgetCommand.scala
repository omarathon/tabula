package uk.ac.warwick.tabula.commands.coursework.assignments


import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AssessmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.AssessmentServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssessmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssessmentServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.web.controllers.StudentCourseworkCommand.StudentAssignments
import uk.ac.warwick.tabula.coursework.web.controllers.{StudentCourseworkCommandInternal, StudentCourseworkCommandHelper}

object StudentCourseworkGadgetCommand {
	def apply(studentCourseYearDetails: StudentCourseYearDetails): Appliable[StudentAssignments] =
		new StudentCourseworkGadgetCommandInternal(studentCourseYearDetails)
			with ComposableCommand[StudentAssignments]
			with AutowiringFeaturesComponent
			with AutowiringAssessmentServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with StudentCourseworkCommandHelper
			with StudentCourseworkGadgetCommandPermissions
			with ReadOnly with Unaudited
}

class StudentCourseworkGadgetCommandInternal(val studentCourseYearDetails: StudentCourseYearDetails)
	extends StudentCourseworkGadgetCommandState with StudentCourseworkCommandInternal{
	self: AssessmentServiceComponent with
		AssessmentMembershipServiceComponent with
		FeaturesComponent with
		StudentCourseworkCommandHelper =>

	override lazy val overridableAssignmentsWithFeedback = {
		assessmentService.getAssignmentsWithFeedback(studentCourseYearDetails)
	}

	override lazy val overridableEnrolledAssignments = {
		val allAssignments = assessmentMembershipService.getEnrolledAssignments(studentCourseYearDetails.studentCourseDetails.student.asSsoUser)
		assessmentService.filterAssignmentsByCourseAndYear(allAssignments, studentCourseYearDetails)
	}

	override lazy val overridableAssignmentsWithSubmission = assessmentService.getAssignmentsWithSubmission(studentCourseYearDetails)

	override val user: User = student.asSsoUser
	override val universityId: String = student.universityId

}

trait StudentCourseworkGadgetCommandState {
	def studentCourseYearDetails: StudentCourseYearDetails
	def student = studentCourseYearDetails.studentCourseDetails.student
}

trait StudentCourseworkGadgetCommandPermissions extends RequiresPermissionsChecking {
	self: StudentCourseworkGadgetCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.Coursework, student)
		p.PermissionCheck(Permissions.Submission.Read, student)
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, student)
		p.PermissionCheck(Permissions.Extension.Read, student)
	}
}
