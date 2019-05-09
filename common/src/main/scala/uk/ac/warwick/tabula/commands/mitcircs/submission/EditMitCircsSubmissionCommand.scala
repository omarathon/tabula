package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.DateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.extensions.{ExtensionPersistenceComponent, HibernateExtensionPersistenceComponent}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.mitcircs.{IssueType, MitCircsContact, MitigatingCircumstancesAffectedAssessment, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.{MitCircsSubmissionReceiptNotification, MitCircsSubmissionUpdatedNotification, MitCircsUpdateOnBehalfNotification}
import uk.ac.warwick.tabula.data.model.{FileAttachment, Notification, StudentMember}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object EditMitCircsSubmissionCommand {
  def apply(submission: MitigatingCircumstancesSubmission, creator: User) =
    new EditMitCircsSubmissionCommandInternal(submission, creator)
      with ComposableCommand[MitigatingCircumstancesSubmission]
      with MitCircsSubmissionValidation
      with MitCircsSubmissionPermissions
      with EditMitCircsSubmissionDescription
      with EditMitCircsSubmissionNotifications
      with MitCircsSubmissionSchedulesNotifications
      with MitCircsSubmissionNotificationCompletion
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with HibernateExtensionPersistenceComponent
}

class EditMitCircsSubmissionCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[MitigatingCircumstancesSubmission] with EditMitCircsSubmissionState with BindListener {

  self: MitCircsSubmissionServiceComponent with ModuleAndDepartmentServiceComponent with ExtensionPersistenceComponent =>

  require(submission.isEditable) // Guarded at controller

  startDate = submission.startDate
  endDate = submission.endDate
  noEndDate = Option(endDate).isEmpty
  issueTypes = submission.issueTypes.asJava
  issueTypeDetails = submission.issueTypeDetails
  reason = submission.reason
  affectedAssessments.addAll(submission.affectedAssessments.asScala.map(new AffectedAssessmentItem(_)).asJava)
  contacted = submission.contacted
  contacts = submission.contacts.asJava
  contactOther = submission.contactOther
  noContactReason = submission.noContactReason
  pendingEvidence = submission.pendingEvidence
  pendingEvidenceDue = submission.pendingEvidenceDue
  attachedFiles = JHashSet(submission.attachments.toSet)
  relatedSubmission = submission.relatedSubmission

  override def onBind(result: BindingResult): Unit = transactional() {
    file.onBind(result)
    affectedAssessments.asScala.foreach(_.onBind(moduleAndDepartmentService))
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

    // TODO dumping the existing ones is a bit wasteful and might cause issues later if we add other props
    submission.affectedAssessments.clear()
    affectedAssessments.asScala.foreach { item =>
      val affected = new MitigatingCircumstancesAffectedAssessment(submission, item)
      submission.affectedAssessments.add(affected)
    }

    submission.pendingEvidence = pendingEvidence
    submission.pendingEvidenceDue = pendingEvidenceDue
    if (submission.attachments != null) {
      // delete attachments that have been removed
      val matchingAttachments: Set[FileAttachment] = submission.attachments.toSet -- attachedFiles.asScala
      matchingAttachments.foreach(delete)
    }
    file.attached.asScala.foreach(submission.addAttachment)

    submission.relatedSubmission = relatedSubmission

    // reset approvedOn when changes are made by others or drafts are saved
    if(isSelf && approve) {
      submission.approveAndSubmit()
    } else if (isSelf) {
      submission.saveAsDraft()
    } else {
      submission.saveOnBehalfOfStudent()
    }

    submission.lastModified = DateTime.now()
    submission.lastModifiedBy = currentUser
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

trait EditMitCircsSubmissionState extends MitCircsSubmissionState {
  val submission: MitigatingCircumstancesSubmission
  lazy val student: StudentMember = submission.student
}

trait EditMitCircsSubmissionNotifications extends Notifies[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission] {

  self: MitCircsSubmissionState =>

  def emit(submission: MitigatingCircumstancesSubmission): Seq[Notification[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]] = {

    val notificationsForStudent = if (!isSelf)  {
      Seq(Notification.init(new MitCircsUpdateOnBehalfNotification, currentUser, submission, submission))
    } else if (approve) {
      Seq(Notification.init(new MitCircsSubmissionReceiptNotification, currentUser, submission, submission))
    } else {
      Nil
    }

    val notificationsForStaff = if (approve) {
      Seq(Notification.init(new MitCircsSubmissionUpdatedNotification, currentUser, submission, submission))
    } else {
      Nil
    }

    notificationsForStudent ++ notificationsForStaff
  }
}
