package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User


object MitCircsSensitiveEvidenceCommand {
  def apply(submission: MitigatingCircumstancesSubmission, currentUser: User) = new MitCircsSensitiveEvidenceCommandInternal(submission, currentUser)
    with ComposableCommand[MitigatingCircumstancesSubmission]
    with MitCircsSensitiveEvidenceValidation
    with MitCircsSensitiveEvidencePermissions
    with MitCircsSensitiveEvidenceDescription
    with AutowiringMitCircsSubmissionServiceComponent
}

class MitCircsSensitiveEvidenceCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[MitigatingCircumstancesSubmission] with MitCircsSensitiveEvidenceState with MitCircsSensitiveEvidenceValidation {

  self: MitCircsSubmissionServiceComponent =>

  sensitiveEvidenceComments = submission.sensitiveEvidenceComments
  sensitiveEvidenceSeenOn = Option(submission.sensitiveEvidenceSeenOn).getOrElse(LocalDate.now)

  def applyInternal(): MitigatingCircumstancesSubmission = {
    submission.sensitiveEvidenceComments = sensitiveEvidenceComments
    submission.sensitiveEvidenceSeenOn = sensitiveEvidenceSeenOn
    submission.sensitiveEvidenceSeenBy = currentUser
    submission.lastModifiedBy = currentUser
    submission.lastModified = DateTime.now()
    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait MitCircsSensitiveEvidencePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: MitCircsSensitiveEvidenceState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.MitigatingCircumstancesSubmission.Modify, submission)
  }
}

trait MitCircsSensitiveEvidenceValidation extends SelfValidating {
  self: MitCircsSensitiveEvidenceState =>
  def validate(errors: Errors) {
    if(!sensitiveEvidenceComments.hasText) {
      errors.rejectValue("sensitiveEvidenceComments", "mitigatingCircumstances.sensitiveEvidenceComments.required")
    }
    if(sensitiveEvidenceSeenOn == null) {
      errors.rejectValue("sensitiveEvidenceSeenOn", "mitigatingCircumstances.sensitiveEvidenceSeenOn.required")
    }
  }
}

trait MitCircsSensitiveEvidenceDescription extends Describable[MitigatingCircumstancesSubmission] {
  self: MitCircsSensitiveEvidenceState =>

  def describe(d: Description) {
    d.mitigatingCircumstancesSubmission(submission)
  }
}

trait MitCircsSensitiveEvidenceState {
  val submission: MitigatingCircumstancesSubmission
  val currentUser: User
  var sensitiveEvidenceComments: String = _
  var sensitiveEvidenceSeenOn: LocalDate = _
}