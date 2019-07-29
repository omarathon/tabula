package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.mitcircs._
import uk.ac.warwick.tabula.data.model.notifications.mitcircs._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import CreateMitCircsSubmissionCommand._

object CreateMitCircsSubmissionCommand {
  type Result = MitigatingCircumstancesSubmission
  type Command =
    Appliable[Result]
      with MitCircsSubmissionState
      with MitCircsSubmissionRequest
      with SelfValidating
      with BindListener
      with Notifies[Result, MitigatingCircumstancesSubmission]
      with SchedulesNotifications[Result, MitigatingCircumstancesSubmission]
      with CompletesNotifications[MitigatingCircumstancesSubmission]

  // Scoped to self
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Modify

  def apply(student: StudentMember, creator: User): Command =
    new CreateMitCircsSubmissionCommandInternal(student, creator)
      with ComposableCommand[Result]
      with MitCircsSubmissionRequest
      with MitCircsSubmissionValidation
      with MitCircsSubmissionPermissions
      with CreateMitCircsSubmissionDescription
      with NewMitCircsSubmissionNotifications
      with MitCircsSubmissionSchedulesNotifications
      with MitCircsSubmissionNotificationCompletion
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
}

class CreateMitCircsSubmissionCommandInternal(val student: StudentMember, val currentUser: User)
  extends CommandInternal[Result]
    with MitCircsSubmissionState
    with BindListener {

  self: MitCircsSubmissionRequest
    with MitCircsSubmissionServiceComponent
    with ModuleAndDepartmentServiceComponent =>

  override def onBind(result: BindingResult): Unit = transactional() {
    result.pushNestedPath("file")
    file.onBind(result)
    result.popNestedPath()

    affectedAssessments.asScala.zipWithIndex.foreach { case (assessment, i) =>
      result.pushNestedPath(s"affectedAssessments[$i]")
      assessment.onBind(moduleAndDepartmentService)
      result.popNestedPath()
    }
  }

  def applyInternal(): Result = transactional() {
    val submission = new MitigatingCircumstancesSubmission(student, currentUser, department)
    submission.startDate = startDate
    submission.endDate = if (noEndDate) null else endDate
    submission.issueTypes = issueTypes.asScala
    if (issueTypes.asScala.contains(IssueType.Other) && issueTypeDetails.hasText) submission.issueTypeDetails = issueTypeDetails else submission.issueTypeDetails = null
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
    submission.pendingEvidence = pendingEvidence
    submission.pendingEvidenceDue = pendingEvidenceDue
    submission.hasSensitiveEvidence = hasSensitiveEvidence
    affectedAssessments.asScala.foreach { item =>
      val affected = new MitigatingCircumstancesAffectedAssessment(submission, item)
      submission.affectedAssessments.add(affected)
    }
    file.attached.asScala.foreach(submission.addAttachment)
    submission.relatedSubmission = relatedSubmission

    if(isSelf && approve) {
      submission.approveAndSubmit()
    } else if (isSelf) {
      submission.saveAsDraft()
    } else {
      submission.saveOnBehalfOfStudent()
    }

    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait MitCircsSubmissionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: MitCircsSubmissionState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(RequiredPermission, MitigatingCircumstancesStudent(student))
  }
}

trait MitCircsSubmissionValidation extends SelfValidating {
  self: MitCircsSubmissionRequest
    with MitCircsSubmissionState
    with ModuleAndDepartmentServiceComponent =>

  override def validate(errors: Errors) {
    // Only validate at submission time, allow drafts to be saved that would be invalid
    if (approve) {
      // validate issue types
      if (issueTypes.isEmpty) errors.rejectValue("issueTypes", "mitigatingCircumstances.issueType.required")
      else if (issueTypes.contains(IssueType.Other) && !issueTypeDetails.hasText)
        errors.rejectValue("issueTypeDetails", "mitigatingCircumstances.issueTypeDetails.required")

      // validate dates
      if (startDate == null) errors.rejectValue("startDate", "mitigatingCircumstances.startDate.required")
      else if (endDate == null && !noEndDate) errors.rejectValue("endDate", "mitigatingCircumstances.endDate.required")
      else if (!noEndDate && endDate.isBefore(startDate)) errors.rejectValue("endDate", "mitigatingCircumstances.endDate.after")

      // validate related submissions
      if (Option(relatedSubmission).exists(_.student != student))
        errors.rejectValue("relatedSubmission", "mitigatingCircumstances.relatedSubmission.sameUser")

      // validate affected assessments
      affectedAssessments.asScala.zipWithIndex.foreach { case (item, index) =>
        errors.pushNestedPath(s"affectedAssessments[$index]")

        if (!item.moduleCode.hasText)
          errors.rejectValue("moduleCode", "mitigatingCircumstances.affectedAssessments.moduleCode.required")
        else if (
          item.moduleCode != MitigatingCircumstancesAffectedAssessment.EngagementCriteriaModuleCode &&
            item.moduleCode != MitigatingCircumstancesAffectedAssessment.OtherModuleCode &&
            moduleAndDepartmentService.getModuleByCode(Module.stripCats(item.moduleCode).getOrElse(item.moduleCode)).isEmpty
        ) errors.rejectValue("moduleCode", "mitigatingCircumstances.affectedAssessments.moduleCode.notFound")

        if (item.academicYear == null) errors.rejectValue("academicYear", "mitigatingCircumstances.affectedAssessments.academicYear.notFound")

        if (!item.name.hasText) errors.rejectValue("name", "mitigatingCircumstances.affectedAssessments.name.required")

        if (item.assessmentType == null) errors.rejectValue("assessmentType", "mitigatingCircumstances.affectedAssessments.assessmentType.notFound")

        errors.popNestedPath()
      }

      if (affectedAssessments.isEmpty)
        errors.rejectValue("affectedAssessments", "mitigatingCircumstances.affectedAssessments.required")

      // validate contact
      Option(contacted).map(_.booleanValue) match {
        case Some(true) =>
          if (contacts.isEmpty)
            errors.rejectValue("contacts", "mitigatingCircumstances.contacts.required")
          else if (contacts.contains(MitCircsContact.Other) && !contactOther.hasText)
            errors.rejectValue("contactOther", "mitigatingCircumstances.contactOther.required")
        case Some(false) =>
          if (!noContactReason.hasText)
            errors.rejectValue("noContactReason", "mitigatingCircumstances.noContactReason.required")
        case None =>
          errors.rejectValue("contacted", "mitigatingCircumstances.contacted.required")
      }

      // validate reason
      if (!reason.hasText) errors.rejectValue("reason", "mitigatingCircumstances.reason.required")

      // validate evidence
      if (attachedFiles.isEmpty && file.attached.isEmpty && pendingEvidence.isEmpty && !hasSensitiveEvidence && !Option(relatedSubmission).exists(_.hasEvidence)) {
        errors.rejectValue("file.upload", "mitigatingCircumstances.evidence.required")
        errors.rejectValue("pendingEvidence", "mitigatingCircumstances.evidence.pending")
      }

      // validate pending evidence
      if (!pendingEvidence.hasText && pendingEvidenceDue != null) {
        errors.rejectValue("pendingEvidence", "mitigatingCircumstances.pendingEvidence.required")
      } else if (pendingEvidence.hasText && pendingEvidenceDue == null) {
        errors.rejectValue("pendingEvidenceDue", "mitigatingCircumstances.pendingEvidenceDue.required")
      }

      if (pendingEvidenceDue != null && !pendingEvidenceDue.isAfter(LocalDate.now)) {
        errors.rejectValue("pendingEvidenceDue", "mitigatingCircumstances.pendingEvidenceDue.future")
      }
    }

  }
}

trait CreateMitCircsSubmissionDescription extends Describable[Result] {
  self: MitCircsSubmissionState =>

  override lazy val eventName: String = "CreateMitCircsSubmission"

  def describe(d: Description) {
    d.member(student)
  }
}

trait MitCircsSubmissionRequest {
  self: MitCircsSubmissionState =>

  var startDate: LocalDate = _
  var endDate: LocalDate = _
  var noEndDate: Boolean = _

  @BeanProperty var issueTypes: JList[IssueType] = JArrayList()
  @BeanProperty var issueTypeDetails: String = _

  var reason: String = _

  var affectedAssessments: JList[AffectedAssessmentItem] = LazyLists.create[AffectedAssessmentItem]()

  var contacted: JBoolean = _
  var contacts: JList[MitCircsContact] = JArrayList()
  var contactOther: String = _
  var noContactReason: String = _

  var hasSensitiveEvidence: Boolean = _
  var pendingEvidence: String = _
  var pendingEvidenceDue: LocalDate = _

  var file: UploadedFile = new UploadedFile
  var attachedFiles: JSet[FileAttachment] = JSet()

  var relatedSubmission: MitigatingCircumstancesSubmission = _
  var approve: Boolean = _ // set this to true when a user is approving a draft submission or one made on their behalf
}

trait MitCircsSubmissionState {
  val student: StudentMember
  val currentUser: User
  lazy val isSelf: Boolean = currentUser.getWarwickId.maybeText.contains(student.universityId)
  lazy val department: Department = student.mostSignificantCourse.department.subDepartmentsContaining(student).filter(_.enableMitCircs).lastOption.getOrElse(
    throw new IllegalArgumentException("Unable to create a mit circs submission for a student who's department doesn't have mit circs enabled")
  )
}

class AffectedAssessmentItem {
  def this(assessment: MitigatingCircumstancesAffectedAssessment) {
    this()
    this.moduleCode = assessment.moduleCode
    this.module = assessment.module
    this.sequence = assessment.sequence
    this.academicYear = assessment.academicYear
    this.name = assessment.name
    this.assessmentType = assessment.assessmentType
    this.deadline = assessment.deadline
    this.boardRecommendations = assessment.boardRecommendations.asJava
    this.acuteOutcomeApplies = Option(assessment.acuteOutcome).isDefined
    this.extensionDeadline = assessment.extensionDeadline.orNull
  }

  var moduleCode: String = _
  var module: Module = _
  var sequence: String = _
  var academicYear: AcademicYear = _
  var name: String = _
  var assessmentType: AssessmentType = _
  var deadline: LocalDate = _
  var boardRecommendations: JList[AssessmentSpecificRecommendation] = JArrayList()
  var acuteOutcomeApplies: Boolean = _
  var extensionDeadline: DateTime = _

  def onBind(moduleAndDepartmentService: ModuleAndDepartmentService): Unit = {
    if (moduleCode != MitigatingCircumstancesAffectedAssessment.EngagementCriteriaModuleCode && moduleCode != MitigatingCircumstancesAffectedAssessment.OtherModuleCode) {
      this.module = moduleAndDepartmentService.getModuleByCode(Module.stripCats(moduleCode).getOrElse(moduleCode))
        .getOrElse(throw new IllegalArgumentException(s"Couldn't find a module with code $moduleCode"))
    } else {
      this.module = null
    }
  }
}

trait NewMitCircsSubmissionNotifications extends Notifies[Result, MitigatingCircumstancesSubmission] {

  self: MitCircsSubmissionRequest with MitCircsSubmissionState =>

  def emit(submission: Result): Seq[Notification[Result, MitigatingCircumstancesSubmission]] = {

    val notificationsForStudent = if (!isSelf)  {
      Seq(Notification.init(new MitCircsSubmissionOnBehalfNotification, currentUser, submission, submission))
    } else if (approve) {
      Seq(Notification.init(new MitCircsSubmissionReceiptNotification, currentUser, submission, submission))
    } else {
      Nil
    }

    val notificationsForStaff = if (approve) {
      Seq(Notification.init(new NewMitCircsSubmissionNotification, currentUser, submission, submission))
    } else {
      Nil
    }

    notificationsForStudent ++ notificationsForStaff
  }
}

trait MitCircsSubmissionSchedulesNotifications extends SchedulesNotifications[Result, MitigatingCircumstancesSubmission] {

  override def transformResult(submission: Result): Seq[MitigatingCircumstancesSubmission] = Seq(submission)

  override def scheduledNotifications(submission: MitigatingCircumstancesSubmission): Seq[ScheduledNotification[MitigatingCircumstancesSubmission]] = {
    val pendingEvidenceReminders =
      if (submission.isEvidencePending) {
        Seq(-1, 0, 1, 4, 7, 10, 13, 16)
          .map(day => submission.pendingEvidenceDue.plusDays(day).toDateTimeAtStartOfDay)
          .filter(_.isAfterNow)
          .map(when => new ScheduledNotification[MitigatingCircumstancesSubmission]("PendingEvidenceReminder", submission, when))
      } else {
        Nil
      }

    val draftReminders =
      if (submission.state == MitigatingCircumstancesSubmissionState.Draft || submission.state == MitigatingCircumstancesSubmissionState.CreatedOnBehalfOfStudent) {
        (1 to 12)
          .map(week => submission.lastModified.plusDays(week * 7))
          .filter(_.isAfterNow)
          .map(when => new ScheduledNotification[MitigatingCircumstancesSubmission]("DraftSubmissionReminder", submission, when))
      } else {
        Nil
      }

    pendingEvidenceReminders ++ draftReminders
  }
}

trait MitCircsSubmissionNotificationCompletion extends CompletesNotifications[Result] {
  self: NotificationHandling =>
  def currentUser: User

  def notificationsToComplete(submission: Result): CompletesNotificationsResult = {
    if (submission.hasEvidence) {
      CompletesNotificationsResult(
        notificationService.findActionRequiredNotificationsByEntityAndType[PendingEvidenceReminderNotification](submission) ++
        notificationService.findActionRequiredNotificationsByEntityAndType[DraftSubmissionReminderNotification](submission),
        currentUser
      )
    } else {
      EmptyCompletesNotificationsResult
    }
  }
}
