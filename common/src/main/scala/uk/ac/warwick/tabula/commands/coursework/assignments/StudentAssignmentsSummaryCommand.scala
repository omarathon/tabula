package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.CourseworkHomepageCommand.StudentAssignmentSummaryInformation
import uk.ac.warwick.tabula.commands.cm2.StudentAssignmentsSummary
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AssessmentServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object StudentAssignmentsSummaryCommand {


  def apply(student: MemberOrUser, academicYear: Option[AcademicYear] = None) =
    new StudentAssignmentsSummaryCommandInternal(student, academicYear)
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringAssessmentServiceComponent
      with ComposableCommand[StudentAssignmentSummaryInformation]
      with StudentAssignmentsSummary
      with StudentAssignmentsSummaryPermissions
      with StudentAssignmentsSummaryCommandState
      with ReadOnly with Unaudited
}


class StudentAssignmentsSummaryCommandInternal(val student: MemberOrUser, val academicYear: Option[AcademicYear])
  extends CommandInternal[StudentAssignmentSummaryInformation] with StudentAssignmentsSummaryCommandState with TaskBenchmarking {

  self: StudentAssignmentsSummary with AssessmentMembershipServiceComponent with AssessmentServiceComponent =>

  override def applyInternal(): StudentAssignmentSummaryInformation = {
    studentInformation
  }

}

trait StudentAssignmentsSummaryPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: StudentAssignmentsSummaryCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    val member = mandatory(student.asMember)
    p.PermissionCheck(Permissions.Profiles.Read.Coursework, member)
    p.PermissionCheck(Permissions.Submission.Read, member)
    p.PermissionCheck(Permissions.Feedback.Read, member)
    p.PermissionCheck(Permissions.Extension.Read, member)
  }

}

trait StudentAssignmentsSummaryCommandState {
  def student: MemberOrUser
  lazy val user: User = student.asUser
  def academicYear: Option[AcademicYear]
}
