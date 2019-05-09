package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

object MitCircsPendingEvidenceCommand {
  def apply(submission: MitigatingCircumstancesSubmission, currentUser: User) = new MitCircsPendingEvidenceCommandInternal(submission, currentUser)
    with ComposableCommand[MitigatingCircumstancesSubmission]
    with MitCircsPendingEvidenceValidation
    with MitCircsPendingEvidencePermissions
    with MitCircsPendingEvidenceDescription
    with AutowiringMitCircsSubmissionServiceComponent
}

class MitCircsPendingEvidenceCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[MitigatingCircumstancesSubmission] with MitCircsPendingEvidenceState with MitCircsPendingEvidenceValidation with BindListener {

  self: MitCircsSubmissionServiceComponent =>

  override def onBind(result: BindingResult): Unit = transactional() {
    file.onBind(result)
  }

  def applyInternal(): MitigatingCircumstancesSubmission = {
    // if new files have been added reset pending evidence
    if(morePending) {
      submission.pendingEvidenceDue = pendingEvidenceDue
      submission.pendingEvidence = pendingEvidence
    } else {
      submission.pendingEvidenceDue = null
      submission.pendingEvidence = null
    }

    file.attached.asScala.foreach(submission.addAttachment)
    submission.lastModified = DateTime.now()
    submission.lastModifiedBy = currentUser
    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait MitCircsPendingEvidencePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: MitCircsPendingEvidenceState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.MitigatingCircumstancesSubmission.Modify, submission)
  }
}

trait MitCircsPendingEvidenceValidation extends SelfValidating {
  self: MitCircsPendingEvidenceState =>
  def validate(errors: Errors) {

    if(file.attached.isEmpty)
      errors.rejectValue("file.upload", "file.missing")

    if(morePending) {
      if(!pendingEvidence.hasText) {
        errors.rejectValue("pendingEvidence", "mitigatingCircumstances.pendingEvidence.required")
      }
      if (pendingEvidenceDue == null) {
        errors.rejectValue("pendingEvidenceDue", "mitigatingCircumstances.pendingEvidenceDue.required")
      } else if(!pendingEvidenceDue.isAfter(LocalDate.now)) {
        errors.rejectValue("pendingEvidenceDue", "mitigatingCircumstances.pendingEvidenceDue.future")
      }
    }
  }
}

trait MitCircsPendingEvidenceDescription extends Describable[MitigatingCircumstancesSubmission] {
  self: MitCircsPendingEvidenceState =>

  def describe(d: Description) {
    d.mitigatingCircumstancesSubmission(submission)
  }
}

trait MitCircsPendingEvidenceState {
  val submission: MitigatingCircumstancesSubmission
  val currentUser: User

  var file: UploadedFile = new UploadedFile
  var morePending: JBoolean = _
  var pendingEvidence: String = _
  var pendingEvidenceDue: LocalDate = _
}