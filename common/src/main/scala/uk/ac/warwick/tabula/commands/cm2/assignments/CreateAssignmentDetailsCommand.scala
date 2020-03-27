package uk.ac.warwick.tabula.commands.cm2.assignments

import javax.validation.constraints.NotEmpty
import org.hibernate.validator.constraints.Length
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeConstants, LocalDate, LocalTime}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{CreatesMarkingWorkflow, ModifyMarkingWorkflowRequest, ModifyMarkingWorkflowState, ModifyMarkingWorkflowValidation}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.data.model.triggers.{AssignmentClosedTrigger, Trigger}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

import scala.jdk.CollectionConverters._

object CreateAssignmentDetailsCommand {
  type Command =
    Appliable[Assignment]
      with ModifyAssignmentDetailsCommandState
      with CreateAssignmentDetailsRequest
      with CreateAssignmentDetailsPrefill
      with SelfValidating
      with SchedulesNotifications[Assignment, Assignment]
      with GeneratesTriggers[Assignment]

  def apply(module: Module, academicYear: AcademicYear): Command =
    new CreateAssignmentDetailsCommandInternal(module, academicYear)
      with ComposableCommand[Assignment]
      with CreateAssignmentDetailsRequest
      with CreateAssignmentDetailsPrefill
      with CreateAssignmentPermissions
      with CreateAssignmentDetailsDescription
      with CreateAssignmentDetailsValidation
      with ModifyAssignmentScheduledNotifications
      with AutowiringAssessmentServiceComponent
      with ModifyAssignmentsDetailsTriggers
      with AutowiringUserLookupComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
}

abstract class CreateAssignmentDetailsCommandInternal(val module: Module, val academicYear: AcademicYear)
  extends CommandInternal[Assignment]
    with ModifyAssignmentDetailsCommandState
    with AssignmentDetailsCopy
    with CreatesMarkingWorkflow {
  self: CreateAssignmentDetailsRequest
    with AssessmentServiceComponent
    with UserLookupComponent
    with CM2MarkingWorkflowServiceComponent =>

  override def applyInternal(): Assignment = {
    val assignment = new Assignment(module)
    // Set default booleans
    BooleanAssignmentProperties(assignment)

    assignment.addDefaultFields()
    copyTo(assignment)
    if (workflowCategory == WorkflowCategory.SingleUse) {
      createAndSaveSingleUseWorkflow(assignment)
    }
    assessmentService.save(assignment)
    assignment
  }
}

trait CreateAssignmentDetailsPrefill {
  self: CreateAssignmentDetailsRequest
    with ModifyAssignmentDetailsCommandState
    with AssessmentServiceComponent =>

  private var _prefilled: Boolean = _
  def prefilled: Boolean = _prefilled

  def prefillFromRecentAssignment(): Unit = {
    if (prefillAssignment != null) {
      copyNonspecificFrom(prefillAssignment)
    } else {
      if (prefillFromRecent) {
        for (a <- assessmentService.recentAssignment(module.adminDepartment)) {
          copyNonspecificFrom(a)
          _prefilled = true
        }
      }
    }
  }

  /**
   * Copy just the fields that it might be useful to
   * prefill. The assignment passed in might typically be
   * another recently created assignment, that may have good
   * initial values for submission options.
   */
  def copyNonspecificFrom(assignment: Assignment): Unit = {
    openDate = Option(assignment.openDate).map(_.toLocalDate).orNull
    closeDate = Option(assignment.closeDate).orNull
    workflowCategory = assignment.workflowCategory.getOrElse(WorkflowCategory.NotDecided)
    if (assignment.workflowCategory.contains(WorkflowCategory.Reusable)) {
      reusableWorkflow = assignment.cm2MarkingWorkflow
    }
    copySharedDetailFrom(assignment)
  }
}

trait AssignmentDetailsCopy {
  self: ModifyAssignmentDetailsRequest
    with ModifyAssignmentDetailsCommandState =>

  def copyTo(assignment: Assignment): Unit = {
    assignment.name = name

    if (assignment.openDate == null || !openDate.isEqual(assignment.openDate.toLocalDate)) {
      assignment.openDate = openDate.toDateTime(Assignment.openTime)
    }

    assignment.academicYear = academicYear
    if (openEnded) {
      assignment.openEndedReminderDate = Option(openEndedReminderDate).map(_.toDateTime(Assignment.openTime)).orNull
      assignment.closeDate = null
    } else {
      assignment.openEndedReminderDate = null

      if (assignment.closeDate == null || !closeDate.isEqual(assignment.closeDate) || !closeDate.toLocalDate.isBefore(Assignment.closeTimeEnforcementDate)) {
        assignment.closeDate = closeDate
      }
    }

    assignment.workflowCategory = Some(workflowCategory)
    if (workflowCategory == WorkflowCategory.Reusable) {
      assignment.cm2MarkingWorkflow = reusableWorkflow
    }
    assignment.anonymity = anonymity
    copySharedDetailTo(assignment)
  }
}

trait ModifyAssignmentDetailsCommandState extends ModifyMarkingWorkflowState {
  self: CM2MarkingWorkflowServiceComponent
    with UserLookupComponent =>

  def module: Module
  def academicYear: AcademicYear
  lazy val department: Department = module.adminDepartment
}

trait ModifyAssignmentDetailsRequest extends SharedAssignmentDetailProperties with ModifyMarkingWorkflowRequest {
  @Length(max = 200)
  @NotEmpty(message = "{NotEmpty.assignmentName}")
  var name: String = _

  var openDate: LocalDate = LocalDate.now

  var closeDate: DateTime = openDate.plusWeeks(2).toDateTime(new LocalTime(12, 0))

  var openEndedReminderDate: LocalDate = _

  var workflowCategory: WorkflowCategory = WorkflowCategory.NotDecided

  var reusableWorkflow: CM2MarkingWorkflow = _

  var anonymity: AssignmentAnonymity = _

  def copyModifyAssignmentDetailsRequestFrom(other: ModifyAssignmentDetailsRequest): Unit = {
    name = other.name
    openDate = other.openDate
    closeDate = other.closeDate
    openEndedReminderDate = other.openEndedReminderDate
    workflowCategory = other.workflowCategory
    reusableWorkflow = other.reusableWorkflow
    anonymity = other.anonymity

    copyBooleanAssignmentDetailPropertiesFrom(other)
    copyModifyMarkingWorkflowRequestFrom(other)
  }
}

trait CreateAssignmentDetailsRequest extends ModifyAssignmentDetailsRequest {
  // can be set to false if that's not what you want.
  var prefillFromRecent = true
  var prefillAssignment: Assignment = _

  def copyCreateAssignmentDetailsRequestFrom(other: CreateAssignmentDetailsRequest): Unit = {
    prefillFromRecent = other.prefillFromRecent
    prefillAssignment = other.prefillAssignment

    copyModifyAssignmentDetailsRequestFrom(other)
  }
}

trait ModifyAssignmentDetailsValidation extends SelfValidating with ModifyMarkingWorkflowValidation {
  self: ModifyAssignmentDetailsRequest
    with UserLookupComponent =>

  private[this] lazy val holidayDates: Seq[LocalDate] = new WorkingDaysHelperImpl().getHolidayDates.asScala.toSeq.map(_.asJoda).sorted

  // validation shared between add and edit
  def genericValidate(errors: Errors): Unit = {
    if (openDate == null) {
      errors.rejectValue("openDate", "openDate.missing")
    }

    if (!openEnded) {
      if (closeDate == null) {
        errors.rejectValue("closeDate", "closeDate.missing")
      } else if (openDate != null && openDate.isAfter(closeDate.toLocalDate)) {
        errors.rejectValue("closeDate", "closeDate.early")
      }
    }
    if (workflowCategory == WorkflowCategory.Reusable && reusableWorkflow == null) {
      errors.rejectValue("reusableWorkflow", "markingWorkflow.reusableWorkflow.none")
    } else if (workflowCategory == WorkflowCategory.SingleUse) {
      if (workflowType == null) {
        errors.rejectValue("workflowType", "markingWorkflow.workflowType.none")
      } else if ((workflowType.name == "DoubleBlind") && (markersA.size() <= 1)) {
        errors.rejectValue("markersA", "NotEnough.markersA", Array("two"), "")
      } else {
        markerValidation(errors, workflowType)
      }
    }
  }

  // Validation shared between add and edit but may be opt in (i.e. may not validate on edit if it hasn't changed)
  def validateOpenDate(errors: Errors): Unit = {
    if (openDate != null) {
      if (holidayDates.contains(openDate) || openDate.getDayOfWeek == DateTimeConstants.SATURDAY || openDate.getDayOfWeek == DateTimeConstants.SUNDAY) {
        errors.rejectValue("openDate", "openDate.notWorkingDay")
      }
    }
  }

  // Validation shared between add and edit but may be opt in (i.e. may not validate on edit if it hasn't changed)
  def validateCloseDate(errors: Errors): Unit = {
    if (closeDate != null && !openEnded) {
      if (holidayDates.contains(closeDate.toLocalDate) || closeDate.getDayOfWeek == DateTimeConstants.SATURDAY || closeDate.getDayOfWeek == DateTimeConstants.SUNDAY) {
        errors.rejectValue("closeDate", "closeDate.notWorkingDay")
      }
      if(!Assignment.isValidCloseTime(closeDate)) {
        val formatter = DateTimeFormat.forPattern("ha")
        val times: Array[AnyRef] = Array(formatter.print(Assignment.CloseTimeStart).toLowerCase, formatter.print(Assignment.CloseTimeEnd).toLowerCase)
        errors.rejectValue("closeDate", "closeDate.invalidTime", times, "")
      }
    }
  }
}

trait CreateAssignmentDetailsValidation extends ModifyAssignmentDetailsValidation {
  self: CreateAssignmentDetailsRequest
    with ModifyAssignmentDetailsCommandState
    with AssessmentServiceComponent
    with UserLookupComponent =>

  def validateCreateAssignmentDetails(errors: Errors): Unit = {
    // TAB-255 Guard to avoid SQL error - if it's null or gigantic it will fail validation in other ways.
    if (name != null && name.length < 3000) {
      val duplicates = assessmentService.getAssignmentByNameYearModule(name, academicYear, module).filter(_.isAlive)
      for (duplicate <- duplicates.headOption) {
        errors.rejectValue("name", "name.duplicate.assignment", Array(duplicate.name), "")
      }
    }

    genericValidate(errors)

    // We always validate open and close dates for new assignments
    validateOpenDate(errors)
    validateCloseDate(errors)
  }

  // Don't add things here as a Command might mix in multiple validators, add to validateCreateAssignmentDetails
  override def validate(errors: Errors): Unit = validateCreateAssignmentDetails(errors)
}

trait CreateAssignmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ModifyAssignmentDetailsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(Permissions.Assignment.Create, mandatory(module))
}

trait CreateAssignmentDetailsDescription extends Describable[Assignment] {
  self: CreateAssignmentDetailsRequest
    with ModifyAssignmentDetailsCommandState =>

  override lazy val eventName = "AddAssignmentDetails"

  override def describe(d: Description): Unit =
    d.module(module).properties(
      "name" -> name,
      "openDate" -> Option(openDate).map(_.toString()).orNull,
      "closeDate" -> Option(closeDate).map(_.toString()).orNull,
      "workflowCtg" -> Option(workflowCategory).map(_.code).orNull,
      "workflowType" -> Option(workflowType).map(_.name).orNull,
      "anonymity" -> Option(anonymity).map(_.code).orNull
    )
}

trait GeneratesNotificationsForAssignment {

  def generateNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = {
    // if the assignment doesn't collect submissions or is open ended then don't schedule any notifications about deadlines
    if (!assignment.collectSubmissions || assignment.openEnded) {
      Seq()
    } else {
      val dayOfDeadline = Assignment.onTheDayReminderDateTime(assignment.closeDate)

      val submissionNotifications = {
        // skip the week late notification if late submission isn't possible
        val daysToSend = if (assignment.allowLateSubmissions) {
          Seq(-7, -1, 1, 7)
        } else {
          Seq(-7, -1, 1)
        }

        val surroundingTimes = for (day <- daysToSend) yield assignment.closeDate.plusDays(day)
        val proposedTimes = Seq(dayOfDeadline) ++ surroundingTimes

        // Filter out all times that are in the past. This should only generate ScheduledNotifications for the future.
        val allTimes = proposedTimes.filter(_.isAfterNow)

        allTimes.map {
          when =>
            new ScheduledNotification[Assignment]("SubmissionDueGeneral", assignment, when)
        }
      }

      val feedbackDeadline = assignment.feedbackDeadline
      val feedbackNotifications =
        if (assignment.dissertation || !assignment.publishFeedback || feedbackDeadline.isEmpty) // No feedback deadline for dissertations or late submissions
          Seq()
        else {
          val daysToSend = Seq(-7, -1, 0)

          val proposedTimes = for (day <- daysToSend) yield feedbackDeadline.get
            .plusDays(day).toDateTimeAtStartOfDay

          // Filter out all times that are in the past. This should only generate ScheduledNotifications for the future.
          val allTimes = proposedTimes.filter(_.isAfterNow)

          allTimes.map {
            when =>
              new ScheduledNotification[Assignment]("FeedbackDueGeneral", assignment, when)
          }
        }

      submissionNotifications ++ feedbackNotifications
    }
  }
}

trait ModifyAssignmentScheduledNotifications
  extends SchedulesNotifications[Assignment, Assignment] with GeneratesNotificationsForAssignment {

  override def transformResult(assignment: Assignment) = Seq(assignment)

  override def scheduledNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = {
    generateNotifications(assignment)
  }

}

trait ModifyAssignmentsDetailsTriggers extends GeneratesTriggers[Assignment] {

  def generateTriggers(commandResult: Assignment): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
    if (commandResult.closeDate != null && commandResult.closeDate.isAfterNow) {
      Seq(AssignmentClosedTrigger(commandResult.closeDate, commandResult))
    } else {
      Seq()
    }
  }
}
