package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesStudent, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

// TODO - pointless as is but I imagine that we may want to split submissions into a few lists later so plumbing this in now
case class HomeInformation(
  submissions: Seq[MitigatingCircumstancesSubmission]
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
    HomeInformation(submissions)
  }
}

trait StudentHomePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentHomeCommandState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.MitigatingCircumstancesSubmission.Modify, MitigatingCircumstancesStudent(student))
  }
}

trait StudentHomeCommandState {
  def student: StudentMember
}
