package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.StudentHomeCommand._
import uk.ac.warwick.tabula.data.model.{Assignment, StudentMember}
import uk.ac.warwick.tabula.data.model.mitcircs._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class HomeInformation(
  submissions: Seq[MitigatingCircumstancesSubmission],
  submissionsWithAcuteOutcomes: Seq[AcuteOutcomesInformation],
)

case class AcuteOutcomesInformation(
  submission: MitigatingCircumstancesSubmission,
  affectedAssessments: Map[MitigatingCircumstancesAffectedAssessment, Set[AcuteOutcomesAssignmentInfo]]
)

case class AcuteOutcomesAssignmentInfo (
  assignment: Assignment,
  hasExtension: Boolean
)

object StudentHomeCommand {
  type Result = HomeInformation
  type Command = Appliable[Result] with PermissionsChecking

  def apply(student: StudentMember, user: CurrentUser): Command =
    new StudentHomeCommandInternal(student, user)
      with ComposableCommand[Result]
      with StudentHomePermissions
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with ReadOnly with Unaudited
}

abstract class StudentHomeCommandInternal(val student: StudentMember, val user: CurrentUser)
  extends CommandInternal[Result]
    with StudentHomeCommandState {
  self: MitCircsSubmissionServiceComponent
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): HomeInformation = {
    val isSelf = user.universityId == student.universityId

    // An MCO can only see the submission if it's in a state where a message can be added
    val submissions =
      mitCircsSubmissionService.submissionsForStudent(student)
        .filter { s => isSelf || s.canAddMessage }

    val submissionsWithAcuteOutcomes = submissions.filter {
      s => s.state == MitigatingCircumstancesSubmissionState.OutcomesRecorded && s.isAcute && s.outcomeGrading != MitigatingCircumstancesGrading.Rejected
    }

    val acuteOutcomesInformation = submissionsWithAcuteOutcomes.map(s => {
      val assignmentsWithOutcomes = s.affectedAssessments.asScala.filter(a => Option(a.acuteOutcome).isDefined)
      val years = assignmentsWithOutcomes.map(_.academicYear).toSet
      val assignments = years.flatMap(year => assessmentMembershipService.getEnrolledAssignments(s.student.asSsoUser, Some(year))).filter(_.summative)
      val assessments = assignmentsWithOutcomes.map(affectedAssessment => {
        affectedAssessment -> assignments.filter(affectedAssessment.matches).map(a => {
          val hasExtension = a.approvedExtensions.get(s.student.userId).isDefined
          AcuteOutcomesAssignmentInfo(a, hasExtension)
        })
      }).toMap
      AcuteOutcomesInformation(s, assessments)
    })

    HomeInformation(
      submissions,
      acuteOutcomesInformation
    )
  }
}

trait StudentHomePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentHomeCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheckAny(Seq(
      CheckablePermission(Permissions.MitigatingCircumstancesSubmission.Modify, MitigatingCircumstancesStudent(student)),
      CheckablePermission(Permissions.MitigatingCircumstancesSubmission.ViewOutcomes, student)
    ))
  }
}

trait StudentHomeCommandState {
  def student: StudentMember
  def user: CurrentUser
}
