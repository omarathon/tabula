package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.commands.cm2.assignments.extensions.{ExtensionPersistenceComponent, HibernateExtensionPersistenceComponent}
import uk.ac.warwick.tabula.data.model.{FileAttachment, Notification, StudentMember}
import uk.ac.warwick.tabula.data.model.mitcircs.IssueType.Other
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesAffectedAssessment, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsSubmissionReceiptNotification
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.BindListener

import scala.collection.JavaConverters._
import scala.collection.mutable

object EditMitCircsSubmissionCommand {
  def apply(submission: MitigatingCircumstancesSubmission, creator: User) =
    new EditMitCircsSubmissionCommandInternal(submission, creator)
      with ComposableCommand[MitigatingCircumstancesSubmission]
      with MitCircsSubmissionValidation
      with MitCircsSubmissionPermissions
      with EditMitCircsSubmissionDescription
      with EditMitCircsSubmissionNotifications
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with HibernateExtensionPersistenceComponent
}

class EditMitCircsSubmissionCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[MitigatingCircumstancesSubmission] with EditMitCircsSubmissionState with BindListener {

  self: MitCircsSubmissionServiceComponent with ModuleAndDepartmentServiceComponent with ExtensionPersistenceComponent =>

  startDate = submission.startDate
  endDate = submission.endDate
  noEndDate = Option(endDate).isEmpty
  issueTypes = submission.issueTypes.asJava
  issueTypeDetails = submission.issueTypeDetails
  reason = submission.reason
  affectedAssessments.addAll(submission.affectedAssessments.asScala.map(new AffectedAssessmentItem(_)).asJava)
  attachedFiles = submission.attachments

  override def onBind(result: BindingResult): Unit = transactional() {
    file.onBind(result)
    affectedAssessments.asScala.foreach(_.onBind(moduleAndDepartmentService))
  }

  def applyInternal(): MitigatingCircumstancesSubmission = transactional() {
    submission.startDate = startDate
    submission.endDate = if (noEndDate) null else endDate
    submission.endDate = endDate
    submission.issueTypes = issueTypes.asScala
    if (issueTypes.contains(Other) && issueTypeDetails.hasText) submission.issueTypeDetails = issueTypeDetails else submission.issueTypeDetails = null
    submission.reason = reason

    // TODO dumping the existing ones is a bit wasteful and might cause issues later if we add other props
    submission.affectedAssessments.clear()
    affectedAssessments.asScala.foreach { item =>
      val affected = new MitigatingCircumstancesAffectedAssessment(submission, item)
      submission.affectedAssessments.add(affected)
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
    Seq(Notification.init(new MitCircsSubmissionReceiptNotification, currentUser, submission, submission))
  }
}
