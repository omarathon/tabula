package uk.ac.warwick.tabula.commands.cm2.assignments

import org.apache.commons.collections.Factory
import org.apache.commons.collections.map.LazyMap
import org.hibernate.exception.ConstraintViolationException
import org.joda.time.DateTime
import org.springframework.util.Assert
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{BooleanFormValue, FormValue, SavedFormValue}
import uk.ac.warwick.tabula.data.model.notifications.coursework._
import uk.ac.warwick.tabula.data.model.triggers.{LateSubmissionTrigger, SubmissionBeforeDeadlineTrigger, Trigger}
import uk.ac.warwick.tabula.events.{NotificationHandling, TriggerHandling}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringCourseworkSubmissionServiceComponent, AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser, FeaturesComponent}

import scala.jdk.CollectionConverters._

object SubmitAssignmentCommand {
  type SubmitAssignmentCommand = Appliable[Submission] with SubmitAssignmentRequest

  def self(assignment: Assignment, user: CurrentUser) =
    new SubmitAssignmentCommandInternal(assignment, MemberOrUser(user.profile, user.apparentUser))
      with ComposableCommand[Submission]
      with SubmitAssignmentBinding
      with SubmitAssignmentAsSelfPermissions
      with SubmitAssignmentDescription
      with SubmitAssignmentValidation
      with SubmitAssignmentNotifications
      with SubmitAssignmentTriggers
      with AutowiringSubmissionServiceComponent
      with AutowiringFeaturesComponent
      with AutowiringZipServiceComponent
      with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent

  def onBehalfOf(assignment: Assignment, member: Member) =
    new SubmitAssignmentCommandInternal(assignment, MemberOrUser(member))
      with ComposableCommand[Submission]
      with SubmitAssignmentBinding
      with SubmitAssignmentOnBehalfOfPermissions
      with SubmitAssignmentDescription
      with SubmitAssignmentValidation
      with SubmitAssignmentNotifications
      with SubmitAssignmentTriggers
      with AutowiringSubmissionServiceComponent
      with AutowiringFeaturesComponent
      with AutowiringZipServiceComponent
      with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent

  def onBehalfOfWithSubmittedDateAndDeadline(assignment: Assignment, member: Member, submittedDate: DateTime, submissionDeadline: DateTime) =
    new SubmitAssignmentCommandInternal(assignment, MemberOrUser(member), Some(submittedDate), Some(submissionDeadline))
      with ComposableCommand[Submission]
      with SubmitAssignmentBinding
      with SubmitAssignmentSetSubmittedDatePermissions
      with SubmitAssignmentDescription
      with SubmitAssignmentValidation
      with SubmitAssignmentTriggers
      with AutowiringSubmissionServiceComponent
      with AutowiringFeaturesComponent
      with AutowiringZipServiceComponent
      with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent
}

trait SubmitAssignmentState {
  def assignment: Assignment

  def user: MemberOrUser
}

trait SubmitAssignmentRequest extends SubmitAssignmentState {

  var fields: JMap[String, FormValue] = {
    /*
     * Goes through the assignment's fields building a set of empty FormValue
     * objects that can be attached to the form and used for binding form values.
     * The key is the form field's ID, so binding should be impervious to field reordering,
     * though it will fail if a field is removed between a user loading a submission form
     * and submitting it.
     */

    val fields = JHashMap(assignment.submissionFields.map { field => field.id -> field.blankFormValue }.toMap)

    LazyMap.decorate(fields, new Factory {
      def create() = new FormValue {
        val field = null

        def persist(value: SavedFormValue): Unit = {}
      }
    }).asInstanceOf[JMap[String, FormValue]]
  }

  var useDisability: JBoolean = _

  var reasonableAdjustmentsDeclared: JBoolean = _

  // used as a hint to the view.
  var justSubmitted: Boolean = false

}

abstract class SubmitAssignmentCommandInternal(val assignment: Assignment, val user: MemberOrUser,
  val submittedDate: Option[DateTime] = None, val submissionDeadline: Option[DateTime] = None)
  extends CommandInternal[Submission] with SubmitAssignmentRequest {

  self: SubmissionServiceComponent
    with FeaturesComponent
    with ZipServiceComponent
    with AttendanceMonitoringCourseworkSubmissionServiceComponent
    with TriggerHandling =>

  override def applyInternal(): Submission = transactional() {
    assignment.submissions.asScala.find(_.isForUser(user.asUser)).foreach { existingSubmission =>
      if (assignment.resubmittable(user.asUser) ||
        (assignment.createdByAEP && assignment.allowResubmission && assignment.isAlive && assignment.collectSubmissions && assignment.isOpened)) {
        triggerService.removeExistingTriggers(existingSubmission)
        submissionService.delete(existingSubmission)
      } else { // Validation should prevent ever reaching here.
        throw new IllegalArgumentException("Submission already exists and can't overwrite it")
      }
    }

    val submitterMember = user.asMember

    val submission = new Submission
    submission.assignment = assignment
    submission.submitted = true
    submission.submittedDate = submittedDate.getOrElse(new DateTime)
    submission.usercode = user.usercode
    submission._universityId = user.universityId

    submissionDeadline.foreach(deadline => submission.explicitSubmissionDeadline = deadline)

    val savedValues = fields.asScala.map {
      case (_, submissionValue) =>
        val value = new SavedFormValue()
        value.name = submissionValue.field.name
        value.submission = submission
        submissionValue.persist(value)
        value
    }.toBuffer

    if (features.disabilityOnSubmission && submitterMember.exists {
      case student: StudentMember => student.disability.exists(_.reportable)
      case _ => false
    } && useDisability != null) {
      val useDisabilityValue = new BooleanFormValue(null)
      useDisabilityValue.value = useDisability
      val value = new SavedFormValue
      value.name = Submission.UseDisabilityFieldName
      value.submission = submission
      useDisabilityValue.persist(value)
      savedValues.append(value)
    }

    if (reasonableAdjustmentsDeclared != null) {
      val reasonableAdjustmentsDeclaredValue = new BooleanFormValue(null)
      reasonableAdjustmentsDeclaredValue.value = reasonableAdjustmentsDeclared
      val value = new SavedFormValue
      value.name = Submission.ReasonableAdjustmentsDeclaredFieldName
      value.submission = submission
      reasonableAdjustmentsDeclaredValue.persist(value)
      savedValues.append(value)
    }

    submission.values = savedValues.toSet[SavedFormValue].asJava

    // TAB-413 assert that we have at least one attachment
    Assert.isTrue(
      submission.values.asScala.exists(value => Option(value.attachments).isDefined && !value.attachments.isEmpty),
      "Submission must have at least one attachment"
    )

    zipService.invalidateSubmissionZip(assignment)
    try {
      submissionService.saveSubmission(submission)

      attendanceMonitoringCourseworkSubmissionService.updateCheckpoints(submission)

      submission
    } catch {
      case e: ConstraintViolationException =>
        // TAB-6045 - this can happen with a precisely-timed double-click on the submit button
        // Roll back the transaction and indicate to the caller what has happened
        throw new RapidResubmissionException(e)
    }
  }
}

class RapidResubmissionException(e: Exception) extends RuntimeException("A duplicate submission was saved while preparing to save this submission", e)

trait SubmitAssignmentBinding extends BindListener {
  self: SubmitAssignmentRequest =>

  override def onBind(result: BindingResult): Unit = {
    for ((key, field) <- fields.asScala) {
      result.pushNestedPath(s"fields[$key]")
      field.onBind(result)
      result.popNestedPath()
    }
  }
}

trait SubmitAssignmentAsSelfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: SubmitAssignmentState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Submission.Create, mandatory(assignment))
  }
}

trait SubmitAssignmentOnBehalfOfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: SubmitAssignmentState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Submission.CreateOnBehalfOf, mandatory(assignment))
    p.PermissionCheck(Permissions.Submission.CreateOnBehalfOf, mandatory(user.asMember))
  }
}

trait SubmitAssignmentSetSubmittedDatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: SubmitAssignmentState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Submission.Update, PermissionsTarget.Global)
  }
}

trait SubmitAssignmentValidation extends SelfValidating {
  self: SubmitAssignmentRequest
    with FeaturesComponent =>

  override def validate(errors: Errors): Unit = {
    if (!assignment.isOpened) {
      errors.reject("assignment.submit.notopen")
    }

    if (!assignment.collectSubmissions) {
      errors.reject("assignment.submit.disabled")
    }

    val hasExtension = assignment.isWithinExtension(user.asUser)

    // TODO take into account any specified submittedDate, instead of excluding AEP
    if (!assignment.allowLateSubmissions && assignment.isClosed && !hasExtension && !assignment.createdByAEP ) {
      errors.reject("assignment.submit.closed")
    }

    // HFC-164
    if (assignment.submissions.asScala.exists(_.usercode == user.usercode)) {
      if (assignment.allowResubmission) {
        if (assignment.allowLateSubmissions && (assignment.isClosed && !hasExtension)) {
          errors.reject("assignment.resubmit.closed")
        }
      } else {
        errors.reject("assignment.submit.already")
      }
    }

    // TODO for multiple attachments, check filenames are unique

    // Individually validate all the custom fields
    // If a submitted ID is not found in assignment, it's ignored.
    assignment.submissionFields.foreach { field =>
      errors.pushNestedPath("fields[%s]".format(field.id))
      fields.asScala.get(field.id).foreach {
        field.validate(_, errors)
      }
      errors.popNestedPath()
    }

    if (features.disabilityOnSubmission && user.asMember.exists {
      case student: StudentMember => student.disability.exists(_.reportable)
      case _ => false
    } && useDisability == null) {
      errors.rejectValue("useDisability", "assignment.submit.chooseDisability")
    }
  }
}

trait SubmitAssignmentDescription extends Describable[Submission] {
  self: SubmitAssignmentState =>

  override lazy val eventName: String = "SubmitAssignment"

  override def describe(d: Description): Unit = {
    d.assignment(assignment)

    assignment.submissions.asScala.find(_.usercode == user.usercode).map { existingSubmission =>
      d.properties(
        "existingSubmission" -> existingSubmission.id,
        "existingAttachments" -> existingSubmission.allAttachments.map(_.id)
      )
    }
  }

  override def describeResult(d: Description, s: Submission): Unit =
    d.assignment(assignment)
     .submission(s)
     .fileAttachments(s.allAttachments)
     .property("submissionIsNoteworthy" -> s.isNoteworthy)
}

trait SubmitAssignmentNotifications extends Notifies[Submission, Submission] with CompletesNotifications[Submission] {
  self: SubmitAssignmentState with NotificationHandling =>

  override def emit(submission: Submission): Seq[NotificationWithTarget[Submission, Assignment] with SubmissionNotification] = {
    val studentNotifications =
      if (assignment.isVisibleToStudents)
        Seq(Notification.init(new SubmissionReceiptNotification, user.asUser, Seq(submission), assignment))
      else Seq()

    studentNotifications ++ Seq(Notification.init(new SubmissionReceivedNotification, user.asUser, Seq(submission), assignment))
  }

  override def notificationsToComplete(commandResult: Submission): CompletesNotificationsResult = {
    CompletesNotificationsResult(
      notificationService.findActionRequiredNotificationsByEntityAndType[SubmissionDueGeneralNotification](assignment) ++
        assignment.allExtensions.getOrElse(user.usercode, Nil).flatMap(
          notificationService.findActionRequiredNotificationsByEntityAndType[SubmissionDueWithExtensionNotification]
        ),
      user.asUser
    )
  }
}

trait SubmitAssignmentTriggers extends GeneratesTriggers[Submission] {
  self: TriggerHandling =>

  override def generateTriggers(commandResult: Submission): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
    if (commandResult.isLate) {
      Seq(LateSubmissionTrigger(DateTime.now, commandResult))
    } else { // Treat authorised-late as being before the close date
      Seq(SubmissionBeforeDeadlineTrigger(DateTime.now, commandResult))
    }
  }
}
