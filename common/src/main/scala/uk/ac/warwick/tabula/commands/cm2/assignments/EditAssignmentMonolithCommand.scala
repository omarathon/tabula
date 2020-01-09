package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._

import scala.jdk.CollectionConverters._

/**
 * This command is used for the API that allows assignments to be modified as a one-shot
 * operation, it delegates to the step-by-step commands.
 */
object EditAssignmentMonolithCommand {
  type Command =
    Appliable[Assignment]
      with EditAssignmentDetailsCommandState
      with EditAssignmentMonolithRequest
      with SelfValidating
      with SchedulesNotifications[Assignment, Assignment]
      with GeneratesTriggers[Assignment]

  def apply(assignment: Assignment): Command =
    new EditAssignmentMonolithCommandInternal(assignment)
      with ComposableCommand[Assignment]
      with EditAssignmentMonolithRequest
      with EditAssignmentPermissions
      with EditAssignmentMonolithDescription
      with EditAssignmentMonolithValidation
      with ModifyAssignmentScheduledNotifications
      with ModifyAssignmentsDetailsTriggers
      with AutowiringAssessmentServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringZipServiceComponent
}

abstract class EditAssignmentMonolithCommandInternal(val assignment: Assignment)
  extends CommandInternal[Assignment] with EditAssignmentDetailsCommandState {
  self: EditAssignmentMonolithRequest
    with AssessmentServiceComponent
    with CM2MarkingWorkflowServiceComponent
    with UserLookupComponent =>

  override def applyInternal(): Assignment = transactional() {
    val detailsCommand = EditAssignmentDetailsCommand(assignment)
    detailsCommand.copyEditAssignmentDetailsRequestFrom(this)
    detailsCommand.apply()

    copySharedFeedbackTo(assignment)
    copySharedSubmissionTo(assignment)
    copySharedOptionsTo(assignment)

    val studentsCommand = ModifyAssignmentStudentsCommand(assignment)
    studentsCommand.populate()

    // SharedStudentBooleans
    studentsCommand.anonymity = anonymity
    studentsCommand.hiddenFromStudents = hiddenFromStudents

    studentsCommand.assessmentGroups = assessmentGroups
    studentsCommand.members = members
    studentsCommand.apply()

    assessmentService.save(assignment)
    assignment
  }
}

trait ModifyAssignmentMonolithRequest
  extends ModifyAssignmentDetailsRequest
    with SharedFeedbackProperties
    with SharedAssignmentSubmissionProperties
    with SharedAssignmentOptionsProperties
    with UpdatesStudentMembership
    with SpecifiesGroupType
    with HasAcademicYear {
  self: ZipServiceComponent
    with UserLookupComponent
    with AssessmentMembershipServiceComponent =>

  var hiddenFromStudents: JBoolean = false

  override val updateStudentMembershipGroupIsUniversityIds: Boolean = false
  override val existingMembers: Option[UnspecifiedTypeUserGroup] = None
  override def resitOnly: Boolean = resitAssessment
}

trait EditAssignmentMonolithRequest
  extends ModifyAssignmentMonolithRequest
    with EditAssignmentDetailsRequest {
  self: EditAssignmentDetailsCommandState
    with ZipServiceComponent
    with UserLookupComponent
    with AssessmentMembershipServiceComponent =>

  override lazy val existingGroups: Option[Seq[UpstreamAssessmentGroupInfo]] = Option(assignment).map(_.upstreamAssessmentGroupInfos)

  /**
   * Convert Spring-bound upstream group references to an AssessmentGroup buffer
   */
  override def updateAssessmentGroups(): Unit = {
    assessmentGroups = upstreamGroups.asScala.flatMap(ug => {
      val template = new AssessmentGroup
      template.assessmentComponent = ug.assessmentComponent
      template.occurrence = ug.occurrence
      template.assignment = assignment
      assessmentMembershipService.getAssessmentGroup(template).orElse(Some(template))
    }).distinct.asJava
  }
}

trait EditAssignmentMonolithValidation extends EditAssignmentDetailsValidation with ModifyAssignmentStudentsValidation {
  self: EditAssignmentMonolithRequest
    with EditAssignmentDetailsCommandState
    with AssessmentServiceComponent
    with UserLookupComponent =>

  override def validate(errors: Errors): Unit = {
    validateEditAssignmentDetails(errors)
    validateSharedOptions(errors)
    validateModifyAssignmentStudents(errors)
  }
}

trait EditAssignmentMonolithDescription extends Describable[Assignment] {
  self: EditAssignmentMonolithRequest
    with EditAssignmentDetailsCommandState =>

  override lazy val eventName = "EditAssignmentMonolith"

  override def describe(d: Description): Unit =
    d.assignment(assignment)
}
