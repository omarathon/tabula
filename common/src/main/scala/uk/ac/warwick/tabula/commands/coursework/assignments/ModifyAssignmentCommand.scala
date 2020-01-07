package uk.ac.warwick.tabula.commands.coursework.assignments

import org.hibernate.validator.constraints.{Length, NotEmpty}
import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.SharedAssignmentProperties
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.triggers.{AssignmentClosedTrigger, Trigger}
import uk.ac.warwick.tabula.services.{AssessmentService, AutowiringAssessmentMembershipServiceComponent, AutowiringUserLookupComponent, AutowiringZipServiceComponent}

import scala.jdk.CollectionConverters._


/**
  * Common behaviour
  */
// TODO TAB-7991 - Nuke this
abstract class ModifyAssignmentCommand(val module: Module, val updateStudentMembershipGroupIsUniversityIds: Boolean = false)
  extends Command[Assignment]
    with SharedAssignmentProperties
    with SelfValidating
    with SpecifiesGroupType
    with CurrentAcademicYear
    with ModifyAssignmentCommandNotifications
    with AutowiringUserLookupComponent
    with AutowiringAssessmentMembershipServiceComponent
    with AutowiringZipServiceComponent
    with UpdatesStudentMembership
    with GeneratesTriggers[Assignment] {

  var service: AssessmentService = Wire.auto[AssessmentService]

  def assignment: Assignment

  @Length(max = 200)
  @NotEmpty(message = "{NotEmpty.assignmentName}")
  var name: String = _

  var openDate: DateTime = DateTime.now.withTime(Assignment.openTime)

  var closeDate: DateTime = openDate.plusWeeks(2).withTime(Assignment.closeTime)

  // can be set to false if that's not what you want.
  var prefillFromRecent = true

  var prefillAssignment: Assignment = _

  private var _prefilled: Boolean = _

  def prefilled: Boolean = _prefilled

  var removeWorkflow: Boolean = false

  def validate(errors: Errors): Unit = {

    // TAB-255 Guard to avoid SQL error - if it's null or gigantic it will fail validation in other ways.
    if (name != null && name.length < 3000) {
      val duplicates = service.getAssignmentByNameYearModule(name, academicYear, module).filter { existing => existing.isAlive && !(existing eq assignment) }
      for (duplicate <- duplicates.headOption) {
        errors.rejectValue("name", "name.duplicate.assignment", Array(name), "")
      }
    }

    if (openDate == null) {
      errors.rejectValue("openDate", "openDate.missing")
    }

    if (!openEnded) {
      if (closeDate == null) {
        errors.rejectValue("closeDate", "closeDate.missing")
      } else if (openDate != null && openDate.isAfter(closeDate)) {
        errors.reject("closeDate.early")
      }
    }

    validateShared(errors)
  }

  def copyTo(assignment: Assignment): Unit = {
    assignment.name = name
    assignment.openDate = openDate
    assignment.closeDate = closeDate
    assignment.academicYear = academicYear
    assignment.feedbackTemplate = feedbackTemplate
    if (assignment.cm2MarkingWorkflow != null) {
      assignment.workflowCategory = Option(WorkflowCategory.Reusable)
    } else {
      assignment.workflowCategory = Option(WorkflowCategory.NoneUse)
    }

    assignment.assessmentGroups.clear()
    assignment.assessmentGroups.addAll(assessmentGroups)
    for (group <- assignment.assessmentGroups.asScala if group.assignment == null) {
      group.assignment = assignment
    }

    copySharedTo(assignment: Assignment)
    if (removeWorkflow) {
      assignment.cm2MarkingWorkflow = null
    }

    if (assignment.members == null) assignment.members = UserGroup.ofUsercodes
    assignment.members.copyFrom(members)
  }

  def prefillFromRecentAssignment(): Unit = {
    if (prefillAssignment != null) {
      copyNonspecificFrom(prefillAssignment)
    } else {
      if (prefillFromRecent) {
        for (a <- service.recentAssignment(module.adminDepartment)) {
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
    openDate = assignment.openDate
    closeDate = assignment.closeDate
    copySharedFrom(assignment)
  }

  def copyGroupsFrom(assignment: Assignment): Unit = {
    assessmentGroups = assignment.assessmentGroups
    // TAB-4848 get all the groups that are linked even if they're marked not in use
    upstreamGroups.addAll(allUpstreamGroups.filter { ug =>
      assessmentGroups.asScala.exists(ag => ug.assessmentComponent == ag.assessmentComponent && ag.occurrence == ug.occurrence)
    }.asJava)
  }

  def copyFrom(assignment: Assignment): Unit = {
    name = assignment.name
    academicYear = assignment.academicYear
    feedbackTemplate = assignment.feedbackTemplate
    if (assignment.members != null) {
      members.copyFrom(assignment.members)
    }
    copyNonspecificFrom(assignment)
  }

  val existingGroups: Option[Seq[UpstreamAssessmentGroupInfo]] = Option(assignment).map(_.upstreamAssessmentGroupInfos)
  val existingMembers: Option[UnspecifiedTypeUserGroup] = Option(assignment).map(_.members)

  /**
    * Convert Spring-bound upstream group references to an AssessmentGroup buffer
    */
  def updateAssessmentGroups(): Unit = {
    assessmentGroups = upstreamGroups.asScala.flatMap(ug => {
      val template = new AssessmentGroup
      template.assessmentComponent = ug.assessmentComponent
      template.occurrence = ug.occurrence
      template.assignment = assignment
      assessmentMembershipService.getAssessmentGroup(template) orElse Some(template)
    }).distinct.asJava
  }

  override def generateTriggers(commandResult: Assignment): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
    if (commandResult.closeDate != null && commandResult.closeDate.isAfterNow) {
      Seq(AssignmentClosedTrigger(commandResult.closeDate, commandResult))
    } else {
      Seq()
    }
  }

}

trait SharedAssignmentCommandNotifications {

  def generateScheduledNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = {
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

trait ModifyAssignmentCommandNotifications extends SchedulesNotifications[Assignment, Assignment] with SharedAssignmentCommandNotifications {

  override def transformResult(assignment: Assignment) = Seq(assignment)

  override def scheduledNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = generateScheduledNotifications(assignment)

}
