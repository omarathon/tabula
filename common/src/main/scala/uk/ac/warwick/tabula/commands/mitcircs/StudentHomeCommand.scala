package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesGrading.Rejected
import uk.ac.warwick.tabula.data.model.{Assignment, StudentMember}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesAffectedAssessment, MitigatingCircumstancesStudent, MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesGrading, MitigatingCircumstancesStudent, MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
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
  def apply(student: StudentMember) = new StudentHomeCommandInternal(student)
    with ComposableCommand[HomeInformation]
    with StudentHomePermissions
    with AutowiringMitCircsSubmissionServiceComponent
    with AutowiringAssessmentMembershipServiceComponent
    with ReadOnly with Unaudited
}

abstract class StudentHomeCommandInternal(val student: StudentMember) extends CommandInternal[HomeInformation] with StudentHomeCommandState {
  self: MitCircsSubmissionServiceComponent with AssessmentMembershipServiceComponent =>

  override def applyInternal(): HomeInformation = {
    val submissions = mitCircsSubmissionService.submissionsForStudent(student)
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
}
