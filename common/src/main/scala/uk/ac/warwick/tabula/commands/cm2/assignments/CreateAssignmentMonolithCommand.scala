package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._

import scala.jdk.CollectionConverters._

/**
 * This command is used for the API that allows assignments to be created as a one-shot
 * operation, it delegates to the step-by-step commands.
 */
object CreateAssignmentMonolithCommand {
  type Command =
    Appliable[Assignment]
      with ModifyAssignmentDetailsCommandState
      with CreateAssignmentMonolithRequest
      with SelfValidating
      with SchedulesNotifications[Assignment, Assignment]
      with GeneratesTriggers[Assignment]

  def apply(module: Module): Command =
    new CreateAssignmentMonolithCommandInternal(module)
      with ComposableCommand[Assignment]
      with CreateAssignmentMonolithRequest
      with CreateAssignmentPermissions
      with CreateAssignmentMonolithDescription
      with CreateAssignmentMonolithValidation
      with ModifyAssignmentScheduledNotifications
      with ModifyAssignmentsDetailsTriggers
      with AutowiringAssessmentServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringZipServiceComponent
}

abstract class CreateAssignmentMonolithCommandInternal(val module: Module)
  extends CommandInternal[Assignment] with ModifyAssignmentDetailsCommandState {
  self: CreateAssignmentMonolithRequest
    with AssessmentServiceComponent
    with CM2MarkingWorkflowServiceComponent
    with UserLookupComponent =>

  override def applyInternal(): Assignment = transactional() {
    val detailsCommand = CreateAssignmentDetailsCommand(module, academicYear)
    detailsCommand.copyCreateAssignmentDetailsRequestFrom(this)
    val assignment = detailsCommand.apply()

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

trait CreateAssignmentMonolithRequest
  extends ModifyAssignmentMonolithRequest
    with CreateAssignmentDetailsRequest
    with CurrentAcademicYear {
  self: ZipServiceComponent
    with UserLookupComponent
    with AssessmentMembershipServiceComponent =>

  override val existingGroups: Option[Seq[UpstreamAssessmentGroupInfo]] = None

  /**
   * Convert Spring-bound upstream group references to an AssessmentGroup buffer
   */
  override def updateAssessmentGroups(): Unit = {
    assessmentGroups = upstreamGroups.asScala.flatMap(ug => {
      val template = new AssessmentGroup
      template.assessmentComponent = ug.assessmentComponent
      template.occurrence = ug.occurrence
      Some(template)
    }).distinct.asJava
  }
}

trait CreateAssignmentMonolithValidation extends CreateAssignmentDetailsValidation with ModifyAssignmentStudentsValidation {
  self: CreateAssignmentMonolithRequest
    with ModifyAssignmentDetailsCommandState
    with AssessmentServiceComponent
    with UserLookupComponent =>

  override def validate(errors: Errors): Unit = {
    validateCreateAssignmentDetails(errors)
    validateSharedOptions(errors)
    validateModifyAssignmentStudents(errors)
  }
}

trait CreateAssignmentMonolithDescription extends Describable[Assignment] {
  self: CreateAssignmentMonolithRequest
    with ModifyAssignmentDetailsCommandState =>

  override lazy val eventName = "CreateAssignmentMonolith"

  override def describe(d: Description): Unit =
    d.module(module).property("academicYear", academicYear.toString)
}
