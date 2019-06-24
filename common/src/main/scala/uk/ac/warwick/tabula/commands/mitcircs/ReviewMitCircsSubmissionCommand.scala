package uk.ac.warwick.tabula.commands.mitcircs

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.ReviewMitCircsSubmissionCommand._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.ReasonableAdjustment
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringExtensionServiceComponent, ExtensionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class ReviewMitCircsSubmissionResult(
  submission: MitigatingCircumstancesSubmission,
  reasonableAdjustments: Set[ReasonableAdjustment],
  reasonableAdjustmentsNotes: String,
  otherMitigatingCircumstancesSubmissions: Seq[MitigatingCircumstancesSubmission],
  relevantExtensions: Seq[Extension],
)

object ReviewMitCircsSubmissionCommand {
  type Result = ReviewMitCircsSubmissionResult
  type Command = Appliable[Result]
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(submission: MitigatingCircumstancesSubmission): Command =
    new ReviewMitCircsSubmissionCommandInternal(submission)
      with ComposableCommand[Result]
      with ReviewMitCircsSubmissionPermissions
      with ReviewMitCircsSubmissionDescription
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringExtensionServiceComponent
}

abstract class ReviewMitCircsSubmissionCommandInternal(val submission: MitigatingCircumstancesSubmission)
  extends CommandInternal[Result]
    with ReviewMitCircsSubmissionState {
  self: MitCircsSubmissionServiceComponent
    with ExtensionServiceComponent =>

  override def applyInternal(): ReviewMitCircsSubmissionResult = transactional() {
    submission.lastViewedByOfficer = DateTime.now
    mitCircsSubmissionService.saveOrUpdate(submission)
    // TODO find out why this is necessary for related submissions which themselves are linked to a related submission
    Option(submission.relatedSubmission).foreach(HibernateHelpers.initialiseAndUnproxy)
    HibernateHelpers.initialiseAndUnproxy(submission)

    ReviewMitCircsSubmissionResult(
      submission = submission,
      reasonableAdjustments = submission.student.reasonableAdjustments,
      reasonableAdjustmentsNotes = submission.student.reasonableAdjustmentsNotes,
      otherMitigatingCircumstancesSubmissions =
        mitCircsSubmissionService.submissionsForStudent(submission.student)
          .filter { s => s != submission && !s.isWithdrawn && !s.isDraft },
      relevantExtensions =
        extensionService.getAllExtensionRequests(submission.student.asSsoUser)
          .sortBy { e => e.requestedExpiryDate.orElse(e.expiryDate) },
    )
  }
}

trait ReviewMitCircsSubmissionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ReviewMitCircsSubmissionState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(submission))
}

trait ReviewMitCircsSubmissionDescription extends Describable[Result] {
  self: ReviewMitCircsSubmissionState =>

  override lazy val eventName: String = "ReviewMitCircsSubmission"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
}

trait ReviewMitCircsSubmissionState {
  def submission: MitigatingCircumstancesSubmission
}
