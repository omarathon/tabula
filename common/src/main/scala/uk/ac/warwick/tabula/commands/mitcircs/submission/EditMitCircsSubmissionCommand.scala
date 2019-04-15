package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.commands.cm2.assignments.extensions.{ExtensionPersistenceComponent, HibernateExtensionPersistenceComponent}
import uk.ac.warwick.tabula.data.model.{FileAttachment, Notification, StudentMember}
import uk.ac.warwick.tabula.data.model.mitcircs.{IssueType, MitCircsContact, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.{MitCircsSubmissionReceiptNotification, MitCircsSubmissionUpdatedNotification}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.BindListener

import scala.collection.JavaConverters._
import scala.collection.mutable


object EditMitCircsSubmissionCommand {
  def apply(submission: MitigatingCircumstancesSubmission, creator: User) = new EditMitCircsSubmissionCommandInternal(submission, creator)
    with ComposableCommand[MitigatingCircumstancesSubmission]
    with MitCircsSubmissionValidation
    with MitCircsSubmissionPermissions
    with EditMitCircsSubmissionDescription
    with EditMitCircsSubmissionNotifications
    with AutowiringMitCircsSubmissionServiceComponent
    with HibernateExtensionPersistenceComponent
}

class EditMitCircsSubmissionCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[MitigatingCircumstancesSubmission] with EditMitCircsSubmissionState with BindListener {

  self: MitCircsSubmissionServiceComponent with ExtensionPersistenceComponent =>

  startDate = submission.startDate
  endDate = submission.endDate
  noEndDate = Option(endDate).isEmpty
  issueTypes = submission.issueTypes.asJava
  issueTypeDetails = submission.issueTypeDetails
  reason = submission.reason
  contacted = submission.contacted
  contacts = submission.contacts.asJava
  contactOther = submission.contactOther
  noContactReason = submission.noContactReason
  attachedFiles = submission.attachments

  override def onBind(result: BindingResult): Unit = transactional() {
    file.onBind(result)
  }

  def applyInternal(): MitigatingCircumstancesSubmission = transactional() {
    submission.startDate = startDate
    submission.endDate = if (noEndDate) null else endDate
    submission.endDate = endDate
    submission.issueTypes = issueTypes.asScala
    if (issueTypes.contains(IssueType.Other) && issueTypeDetails.hasText) submission.issueTypeDetails = issueTypeDetails else submission.issueTypeDetails = null
    submission.reason = reason
    submission.contacted = contacted
    if (contacted) {
      submission.noContactReason = null
      submission.contacts = contacts.asScala
      if (contacts.asScala.contains(MitCircsContact.Other) && contactOther.hasText) submission.contactOther = contactOther else submission.contactOther = null
    } else {
      submission.noContactReason = noContactReason
      submission.contacts = Seq()
      submission.contactOther = null
    }

    if (submission.attachments != null) {
      // delete attachments that have been removed
      val matchingAttachments: mutable.Set[FileAttachment] = submission.attachments.asScala -- attachedFiles.asScala
      matchingAttachments.foreach(delete)
    }
    file.attached.asScala.foreach(submission.addAttachment)
    submission.lastModified = DateTime.now()
    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait EditMitCircsSubmissionDescription extends Describable[MitigatingCircumstancesSubmission] {
  self: EditMitCircsSubmissionState =>

  def describe(d: Description) {
    d.member(student)
    d.mitigatingCircumstancesSubmission(submission)
  }
}

trait EditMitCircsSubmissionState extends CreateMitCircsSubmissionState {
  val submission: MitigatingCircumstancesSubmission
  lazy val student: StudentMember = submission.student
}

trait EditMitCircsSubmissionNotifications extends Notifies[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission] {

  self: CreateMitCircsSubmissionState =>

  def emit(submission: MitigatingCircumstancesSubmission): Seq[Notification[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]] = {
    Seq(
      Notification.init(new MitCircsSubmissionReceiptNotification, currentUser, submission, submission),
      Notification.init(new MitCircsSubmissionUpdatedNotification, currentUser, submission, submission)
    )
  }
}
