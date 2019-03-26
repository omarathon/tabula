package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.{Notification, StudentMember}
import uk.ac.warwick.tabula.data.model.mitcircs.IssueType.Other
import uk.ac.warwick.tabula.data.model.mitcircs.{IssueType, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsSubmissionReceiptNotification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.beans.BeanProperty


object CreateMitCircsSubmissionCommand {
  def apply(student: StudentMember, creator: User) = new CreateMitCircsSubmissionCommandInternal(student, creator)
    with ComposableCommand[MitigatingCircumstancesSubmission]
    with CreateMitCircsSubmissionValidation
    with CreateMitCircsSubmissionPermissions
    with CreateMitCircsSubmissionDescription
    with NewMitCircsSubmissionNotifications
    with AutowiringMitCircsSubmissionServiceComponent
}

class CreateMitCircsSubmissionCommandInternal(val student: StudentMember, val creator: User) extends CommandInternal[MitigatingCircumstancesSubmission]
  with CreateMitCircsSubmissionState {

  self: MitCircsSubmissionServiceComponent  =>

  def applyInternal(): MitigatingCircumstancesSubmission = {
    val submission = new MitigatingCircumstancesSubmission(student, creator)
    submission.startDate = startDate
    submission.endDate = endDate
    submission.issueType = issueType
    if (issueType == Other && issueTypeDetails.hasText) submission.issueTypeDetails = issueTypeDetails else issueTypeDetails = null
    submission.reason = reason
    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait CreateMitCircsSubmissionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CreateMitCircsSubmissionState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.MitigatingCircumstancesSubmission.Create, student)
  }
}

trait CreateMitCircsSubmissionValidation extends SelfValidating {
  self: CreateMitCircsSubmissionState =>

  override def validate(errors: Errors) {
    // validate dates
    if(startDate == null) errors.rejectValue("startDate", "mitigatingCircumstances.startDate.required")
    else if(endDate == null) errors.rejectValue("endDate", "mitigatingCircumstances.endDate.required")
    else if(endDate.isBefore(startDate)) errors.rejectValue("endDate", "mitigatingCircumstances.endDate.after")

    // validate issue types
    if(issueType == null) errors.rejectValue("issueType", "mitigatingCircumstances.issueType.required")
    else if(issueType == Other && !issueTypeDetails.hasText)
      errors.rejectValue("issueTypeDetails", "mitigatingCircumstances.issueTypeDetails.required")

    // validate reason
    if(!reason.hasText) errors.rejectValue("reason", "mitigatingCircumstances.reason.required")
  }
}

trait CreateMitCircsSubmissionDescription extends Describable[MitigatingCircumstancesSubmission] {
  self: CreateMitCircsSubmissionState =>

  def describe(d: Description) {
    d.member(student)
  }
}

trait CreateMitCircsSubmissionState {

  val student: StudentMember
  val creator: User

  var startDate: DateTime = _
  var endDate: DateTime = _

  @BeanProperty var issueType: IssueType = _
  @BeanProperty var issueTypeDetails: String = _

  var reason: String = _
}

trait NewMitCircsSubmissionNotifications extends Notifies[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission] {

  self: CreateMitCircsSubmissionState =>

  def emit(submission: MitigatingCircumstancesSubmission): Seq[Notification[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]] = {
    Seq(Notification.init(new MitCircsSubmissionReceiptNotification, creator, submission, submission))
  }
}
