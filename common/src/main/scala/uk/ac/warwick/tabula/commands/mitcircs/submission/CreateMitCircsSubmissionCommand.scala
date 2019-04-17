package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.LocalDate
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JSet
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.mitcircs.{IssueType, MitCircsContact, MitigatingCircumstancesAffectedAssessment, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.{MitCircsSubmissionReceiptNotification, NewMitCircsSubmissionNotification}
import uk.ac.warwick.tabula.data.model.{AssessmentType, Department, FileAttachment, Module, Notification, StudentMember}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

object CreateMitCircsSubmissionCommand {
  def apply(student: StudentMember, creator: User) =
    new CreateMitCircsSubmissionCommandInternal(student, creator)
      with ComposableCommand[MitigatingCircumstancesSubmission]
      with MitCircsSubmissionValidation
      with MitCircsSubmissionPermissions
      with CreateMitCircsSubmissionDescription
      with NewMitCircsSubmissionNotifications
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
}

class CreateMitCircsSubmissionCommandInternal(val student: StudentMember, val currentUser: User) extends CommandInternal[MitigatingCircumstancesSubmission]
  with CreateMitCircsSubmissionState with BindListener {

  self: MitCircsSubmissionServiceComponent with ModuleAndDepartmentServiceComponent =>

  override def onBind(result: BindingResult): Unit = transactional() {
    file.onBind(result)
    affectedAssessments.asScala.foreach(_.onBind(moduleAndDepartmentService))
  }

  def applyInternal(): MitigatingCircumstancesSubmission = transactional() {
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
    submission.stepsSoFar = stepsSoFar
    submission.changeOrResolve = changeOrResolve
    submission.pendingEvidence = pendingEvidence
    affectedAssessments.asScala.foreach { item =>
      val affected = new MitigatingCircumstancesAffectedAssessment(submission, item)
      submission.affectedAssessments.add(affected)
    }
    file.attached.asScala.foreach(submission.addAttachment)
    submission.relatedSubmission = relatedSubmission
    mitCircsSubmissionService.saveOrUpdate(submission)
    submission
  }
}

trait MitCircsSubmissionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CreateMitCircsSubmissionState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.MitigatingCircumstancesSubmission.Modify, student)
  }
}

trait MitCircsSubmissionValidation extends SelfValidating {
  self: CreateMitCircsSubmissionState with ModuleAndDepartmentServiceComponent =>

  override def validate(errors: Errors) {
    // validate dates
    if(startDate == null) errors.rejectValue("startDate", "mitigatingCircumstances.startDate.required")
    else if(endDate == null && !noEndDate) errors.rejectValue("endDate", "mitigatingCircumstances.endDate.required")
    else if(!noEndDate && endDate.isBefore(startDate)) errors.rejectValue("endDate", "mitigatingCircumstances.endDate.after")

    // validate issue types
    if(issueTypes.isEmpty) errors.rejectValue("issueTypes", "mitigatingCircumstances.issueType.required")
    else if(issueTypes.contains(IssueType.Other) && !issueTypeDetails.hasText)
      errors.rejectValue("issueTypeDetails", "mitigatingCircumstances.issueTypeDetails.required")

    // validate contact
    if(contacted) {
      if (contacts.isEmpty)
        errors.rejectValue("contacts", "mitigatingCircumstances.contacts.required")
      else if(contacts.contains(MitCircsContact.Other) && !contactOther.hasText)
        errors.rejectValue("contactOther", "mitigatingCircumstances.contactOther.required")
    } else {
      if(!noContactReason.hasText)
        errors.rejectValue("noContactReason", "mitigatingCircumstances.noContactReason.required")
    }

    // validate reason
    if(!reason.hasText) errors.rejectValue("reason", "mitigatingCircumstances.reason.required")

    if(Option(relatedSubmission).exists(_.student != student))
      errors.rejectValue("relatedSubmission", "mitigatingCircumstances.relatedSubmission.sameUser")

    // validate affected issue types
    affectedAssessments.asScala.zipWithIndex.foreach { case (item, index) =>
      errors.pushNestedPath(s"affectedAssessments[$index]")

      if (!item.moduleCode.hasText)
        errors.rejectValue("moduleCode", "mitigatingCircumstances.affectedAssessments.moduleCode.required")
      else if (moduleAndDepartmentService.getModuleByCode(Module.stripCats(item.moduleCode).getOrElse(item.moduleCode)).isEmpty)
        errors.rejectValue("moduleCode", "mitigatingCircumstances.affectedAssessments.moduleCode.notFound")

      if (item.academicYear == null) errors.rejectValue("academicYear", "mitigatingCircumstances.affectedAssessments.academicYear.notFound")

      if (!item.name.hasText) errors.rejectValue("name", "mitigatingCircumstances.affectedAssessments.name.notFound")

      if (item.assessmentType == null) errors.rejectValue("academicYear", "mitigatingCircumstances.affectedAssessments.assessmentType.notFound")

      errors.popNestedPath()
    }
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
  val currentUser: User
  val department: Department = student.mostSignificantCourse.department.subDepartmentsContaining(student).filter(_.enableMitCircs).lastOption.getOrElse(
    throw new IllegalArgumentException("Unable to create a mit circs submission for a student who's department doesn't have mit circs enabled")
  )

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

  var stepsSoFar: String = _
  var changeOrResolve: String = _
  var pendingEvidence: String = _

  var file: UploadedFile = new UploadedFile
  var attachedFiles: JSet[FileAttachment] = JSet()

  var relatedSubmission: MitigatingCircumstancesSubmission = _
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
  }

  var moduleCode: String = _
  var module: Module = _
  var sequence: String = _
  var academicYear: AcademicYear = _
  var name: String = _
  var assessmentType: AssessmentType = _
  var deadline: LocalDate = _

  def onBind(moduleAndDepartmentService: ModuleAndDepartmentService): Unit = {
    this.module = moduleAndDepartmentService.getModuleByCode(Module.stripCats(moduleCode).getOrElse(moduleCode))
      .getOrElse(throw new IllegalArgumentException(s"Couldn't find a module with code $moduleCode"))
  }
}

trait NewMitCircsSubmissionNotifications extends Notifies[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission] {

  self: CreateMitCircsSubmissionState =>

  def emit(submission: MitigatingCircumstancesSubmission): Seq[Notification[MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmission]] = {
    Seq(
      Notification.init(new MitCircsSubmissionReceiptNotification, currentUser, submission, submission),
      Notification.init(new NewMitCircsSubmissionNotification, currentUser, submission, submission)
    )
  }
}
