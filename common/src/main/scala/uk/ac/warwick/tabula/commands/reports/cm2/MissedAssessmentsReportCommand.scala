package uk.ac.warwick.tabula.commands.reports.cm2

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.reports.{ReportCommandRequest, ReportCommandRequestValidation, ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services._

import scala.collection.JavaConverters._

object MissedAssessmentsReportCommand {
  def apply(department: Department, academicYear: AcademicYear) =
    new MissedAssessmentsReportCommandInternal(department, academicYear)
      with ComposableCommand[MissedAssessmentsReport]
      with AutowiringAssessmentServiceComponent
      with AutowiringProfileServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with ReportPermissions
      with ReportCommandRequestValidation
      with ReadOnly
      with Unaudited
}

class MissedAssessmentsReportCommandInternal(val department: Department, val academicYear: AcademicYear) extends CommandInternal[MissedAssessmentsReport] with ReportCommandRequest with ReportCommandState {
  self: AssessmentServiceComponent with ProfileServiceComponent with AssessmentMembershipServiceComponent =>

  override protected def applyInternal(): MissedAssessmentsReport = transactional(readOnly = true) {
    val assignments = assessmentService.getDepartmentAssignmentsClosingBetween(department, startDate, endDate)
      .filter(_.collectSubmissions)

    val assignmentMembers = assignments.flatMap(assignment => assessmentMembershipService.determineMembershipUsers(assignment).map(user => (assignment, user)))

    val members = profileService.getAllMembersByUsers(assignmentMembers.map(_._2))

    val entities = assignmentMembers.flatMap { case (assignment, user) =>
      members.get(user).flatMap { student =>
        val submission = assignment.submissions.asScala.find(_.isForUser(user))
        val workingDaysLateIfSubmittedNow = assignment.workingDaysLateIfSubmittedNow(user.getUserId)

        if (submission.forall(_.isLate) && workingDaysLateIfSubmittedNow > 0) {
          val extension = assignment.approvedExtensions.values.find(_.isForUser(user))

          Some(MissedAssessmentsReportEntity(
            student = student,
            module = assignment.module,
            assignment = assignment,
            submission = submission,
            extension = extension,
            workingDaysLate = workingDaysLateIfSubmittedNow
          ))
        } else None
      }
    }

    MissedAssessmentsReport(entities)
  }
}

case class MissedAssessmentsReport(entities: Seq[MissedAssessmentsReportEntity])

case class MissedAssessmentsReportEntity(
  student: Member,
  module: Module,
  assignment: Assignment,
  submission: Option[Submission],
  extension: Option[Extension],
  workingDaysLate: Int
)

