package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AssignmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.AssignmentServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssignmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssignmentServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.web.controllers.StudentCourseworkCommand.StudentAssignments

@Controller
@RequestMapping(Array("/student/byCourseAndYear/{studentCourseYearDetails}"))
class StudentCourseworkGadgetController extends StudentCourseworkController {

	@ModelAttribute("command") def command(@PathVariable studentCourseYearDetails: StudentCourseYearDetails) =
		StudentCourseworkGadgetCommand(studentCourseYearDetails)

	@RequestMapping
	def listAssignments(@ModelAttribute("command") command: Appliable[StudentAssignments], @PathVariable studentCourseYearDetails: StudentCourseYearDetails, user: CurrentUser): Mav =
		getMav(studentCourseYearDetails.studentCourseDetails.student, command.apply())

}

object StudentCourseworkGadgetCommand {
	def apply(studentCourseYearDetails: StudentCourseYearDetails): Appliable[StudentAssignments] =
		new StudentCourseworkGadgetCommandInternal(studentCourseYearDetails)
			with ComposableCommand[StudentAssignments]
			with AutowiringFeaturesComponent
			with AutowiringAssignmentServiceComponent
			with AutowiringAssignmentMembershipServiceComponent
			with StudentCourseworkCommandHelper
			with StudentCourseworkGadgetCommandPermissions
			with ReadOnly with Unaudited
}

class StudentCourseworkGadgetCommandInternal(val studentCourseYearDetails: StudentCourseYearDetails)
	extends StudentCourseworkGadgetCommandState with StudentCourseworkCommandInternal{
	self: AssignmentServiceComponent with
		AssignmentMembershipServiceComponent with
		FeaturesComponent with
		StudentCourseworkCommandHelper =>

	override def overridableAssignmentsWithFeedback = {
		assignmentService.getAssignmentsWithFeedback(studentCourseYearDetails)
	}

	override def overridableEnrolledAssignments = {
		val allAssignments = assignmentMembershipService.getEnrolledAssignments(studentCourseYearDetails.studentCourseDetails.student.asSsoUser)
		assignmentService.filterAssignmentsByCourseAndYear(allAssignments, studentCourseYearDetails)
	}

	override def overridableAssignmentsWithSubmission = assignmentService.getAssignmentsWithSubmission(studentCourseYearDetails)

	override def user: User = student.asSsoUser
	override def universityId: String = student.universityId

	def applyInternal() = getAssignments
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
		p.PermissionCheck(Permissions.Feedback.Read, student)
		p.PermissionCheck(Permissions.Extension.Read, student)
	}
}
