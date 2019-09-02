package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.commands.coursework.assignments.StudentCourseworkCommand.StudentAssignments
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, MemberOrUser, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AssessmentServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, FeaturesComponent}
import uk.ac.warwick.userlookup.User

object StudentCourseworkFullScreenCommand {
  def apply(memberOrUser: MemberOrUser, academicYearOption: Option[AcademicYear] = None): Appliable[StudentAssignments] =
    new StudentCourseworkFullScreenCommandInternal(memberOrUser, academicYearOption)
      with ComposableCommand[StudentAssignments]
      with StudentCourseworkFullScreenCommandPermissions
      with AutowiringAssessmentServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringFeaturesComponent
      with StudentCourseworkCommandHelper
      with ReadOnly with Unaudited
}

class StudentCourseworkFullScreenCommandInternal(val memberOrUser: MemberOrUser, val academicYearOption: Option[AcademicYear])
  extends StudentCourseworkCommtandInternal with StudentCourseworkFullScreenCommandState {

  self: AssessmentServiceComponent with
    AssessmentMembershipServiceComponent with
    FeaturesComponent with
    StudentCourseworkCommandHelper =>

  override lazy val overridableAssignmentsWithFeedback: Seq[Assignment] = assessmentService.getAssignmentsWithFeedback(memberOrUser.usercode, academicYearOption)

  override lazy val overridableEnrolledAssignments: Seq[Assignment] = assessmentMembershipService.getEnrolledAssignments(memberOrUser.asUser, academicYearOption)

  override lazy val overridableAssignmentsWithSubmission: Seq[Assignment] = assessmentService.getAssignmentsWithSubmission(memberOrUser.usercode, academicYearOption)

  override val usercode: String = memberOrUser.usercode

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
      p.PermissionCheck(Permissions.AssignmentFeedback.Read, member)
      p.PermissionCheck(Permissions.Extension.Read, member)
    }
  }
}
