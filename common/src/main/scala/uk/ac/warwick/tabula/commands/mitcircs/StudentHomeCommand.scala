package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesGrading, MitigatingCircumstancesStudent, MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class HomeInformation(
  submissions: Seq[MitigatingCircumstancesSubmission],
  submissionsWithAcuteOutcomes: Seq[MitigatingCircumstancesSubmission],
)

object StudentHomeCommand {
  def apply(student: StudentMember) = new StudentHomeCommandInternal(student)
    with ComposableCommand[HomeInformation]
    with StudentHomePermissions
    with AutowiringMitCircsSubmissionServiceComponent
    with ReadOnly with Unaudited
}

abstract class StudentHomeCommandInternal(val student: StudentMember) extends CommandInternal[HomeInformation] with StudentHomeCommandState {
  self: MitCircsSubmissionServiceComponent =>

  override def applyInternal(): HomeInformation = {
    val submissions = mitCircsSubmissionService.submissionsForStudent(student)
    val submissionsWithAcuteOutcomes = submissions.filter {
      s => s.state == MitigatingCircumstancesSubmissionState.OutcomesRecorded && s.isAcute && s.outcomeGrading != MitigatingCircumstancesGrading.Rejected
    }

    HomeInformation(
      submissions,
      submissionsWithAcuteOutcomes
    )
  }
}

trait StudentHomePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentHomeCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheckAny(Seq(
      CheckablePermission(Permissions.MitigatingCircumstancesSubmission.Modify, MitigatingCircumstancesStudent(student)),
      CheckablePermission(Permissions.MitigatingCircumstancesSubmission.ViewOutcomes, MitigatingCircumstancesStudent(student))
    ))
  }
}

trait StudentHomeCommandState {
  def student: StudentMember
}
