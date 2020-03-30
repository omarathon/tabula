package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.extensions.{ExtensionPersistenceComponent, HibernateExtensionPersistenceComponent}
import uk.ac.warwick.tabula.commands.cm2.markingworkflows._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.WorkflowCategory.NoneUse
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.{ModeratedMarking, SelectedModeratedMarking}
import uk.ac.warwick.tabula.data.model.markingworkflow.{CM2MarkingWorkflow, ModeratedWorkflow}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, FeaturesComponent}

import scala.jdk.CollectionConverters._

object EditAssignmentDetailsCommand {
  type Command =
    Appliable[Assignment]
      with EditAssignmentDetailsCommandState
      with EditAssignmentDetailsRequest
      with SelfValidating
      with SchedulesNotifications[Assignment, Assignment]
      with GeneratesTriggers[Assignment]

  def apply(assignment: Assignment): Command =
    new EditAssignmentDetailsCommandInternal(assignment)
      with ComposableCommand[Assignment]
      with EditAssignmentDetailsRequest
      with EditAssignmentPermissions
      with EditAssignmentDetailsDescription
      with EditAssignmentDetailsValidation
      with ModifyAssignmentScheduledNotifications
      with AutowiringAssessmentServiceComponent
      with AutowiringExtensionServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
      with AutowiringFeedbackServiceComponent
      with AutowiringFeaturesComponent
      with HibernateExtensionPersistenceComponent
      with ModifyAssignmentsDetailsTriggers
      with PopulateEditAssignmentDetailsRequest
}

abstract class EditAssignmentDetailsCommandInternal(override val assignment: Assignment)
  extends CommandInternal[Assignment]
    with EditAssignmentDetailsCommandState
    with AssignmentDetailsCopy
    with CreatesMarkingWorkflow {
  self: EditAssignmentDetailsRequest
    with AssessmentServiceComponent
    with UserLookupComponent
    with CM2MarkingWorkflowServiceComponent
    with FeaturesComponent
    with ExtensionPersistenceComponent =>

  override def applyInternal(): Assignment = {
    lazy val moderatedWorkflow = workflow.map(HibernateHelpers.initialiseAndUnproxy).collect { case w: ModeratedWorkflow => w }

    def moderationSelectorChanged: Boolean = features.moderationSelector && moderatedWorkflow.exists(w => w.moderationSampler != sampler)

    if (workflowCategory == WorkflowCategory.SingleUse) {
      workflow.filterNot(_.isReusable) match {
        // update any existing single use workflows
        case Some(w) =>
          if (workflowType != null && (w.workflowType != workflowType || moderationSelectorChanged)) {
            // delete the old workflow and make a new one with the new type
            cm2MarkingWorkflowService.delete(w)
            createAndSaveSingleUseWorkflow(assignment)
            //user has changed workflow category so remove all previous feedbacks
            assignment.resetMarkerFeedback()
          } else {
            w.replaceMarkers(markersAUsers, markersBUsers)
            moderatedWorkflow.filter(_ => features.moderationSelector).foreach(w => w.moderationSampler = sampler)
            cm2MarkingWorkflowService.save(w)
          }
        // persist any new workflow
        case _ =>
          createAndSaveSingleUseWorkflow(assignment)
          assignment.resetMarkerFeedback() // remove any feedback created from the old reusable marking workflow
      }
    } else if ((workflowCategory == WorkflowCategory.NoneUse || workflowCategory == WorkflowCategory.NotDecided) && workflow.isDefined) {
      // before we de-attach, store it to be deleted afterwards
      val existingWorkflow = workflow
      assignment.cm2MarkingWorkflow = null
      existingWorkflow.filterNot(_.isReusable).foreach(cm2MarkingWorkflowService.delete)
      //if we have any prior markerfeedbacks/feedbacks attached -  remove them
      assignment.resetMarkerFeedback()
    } else if (workflowCategory == WorkflowCategory.Reusable) {
      if (reusableWorkflow != null && assignment.cm2MarkingWorkflow != null && reusableWorkflow.workflowType != assignment.cm2MarkingWorkflow.workflowType) {
        assignment.resetMarkerFeedback()
      }
      workflow.filterNot(_.isReusable).foreach(cm2MarkingWorkflowService.delete)
    }

    copyTo(assignment)

    if (openEnded) {
      assignment.removeRejectedExtensions(self)
    }

    assessmentService.save(assignment)
    assignment
  }

}

trait EditAssignmentDetailsCommandState extends ModifyAssignmentDetailsCommandState with EditMarkingWorkflowState {
  self: UserLookupComponent
    with CM2MarkingWorkflowServiceComponent =>

  def assignment: Assignment
  def academicYear: AcademicYear = assignment.academicYear
  def module: Module = assignment.module
  def workflow: Option[CM2MarkingWorkflow] = Option(assignment.cm2MarkingWorkflow)
}

trait EditAssignmentDetailsRequest extends ModifyAssignmentDetailsRequest {
  def copyEditAssignmentDetailsRequestFrom(other: EditAssignmentDetailsRequest): Unit =
    copyModifyAssignmentDetailsRequestFrom(other)
}

trait PopulateEditAssignmentDetailsRequest {
  self: EditAssignmentDetailsCommandState
    with EditAssignmentDetailsRequest =>

  name = assignment.name
  openDate = Option(assignment.openDate).map(_.toLocalDate).orNull
  openEnded = assignment.openEnded
  resitAssessment = assignment.resitAssessment
  openEndedReminderDate = Option(assignment.openEndedReminderDate).map(_.toLocalDate).orNull
  closeDate = Option(assignment.closeDate).orNull
  workflowCategory = assignment.workflowCategory.getOrElse(WorkflowCategory.NotDecided)
  reusableWorkflow = Option(assignment.cm2MarkingWorkflow).filter(_.isReusable).orNull
  anonymity = assignment._anonymity
  workflow.foreach(w =>
    if (w.workflowType == SelectedModeratedMarking) workflowType = ModeratedMarking // we don't show admin moderation as a separate option in the UI
    else workflowType = w.workflowType
  )
  workflow.map(HibernateHelpers.initialiseAndUnproxy).collect { case w: ModeratedWorkflow => w }.foreach(w =>
    sampler = w.moderationSampler
  )
  extractMarkers match {
    case (a, b) =>
      markersA = JArrayList(a)
      markersB = JArrayList(b)
  }
}

trait EditAssignmentDetailsValidation extends ModifyAssignmentDetailsValidation {
  self: EditAssignmentDetailsRequest
    with EditAssignmentDetailsCommandState
    with AssessmentServiceComponent
    with UserLookupComponent =>

  def validateEditAssignmentDetails(errors: Errors): Unit = {
    if (name != null && name.length < 3000) {
      val duplicates = assessmentService.getAssignmentByNameYearModule(name, academicYear, module).filter { existing => existing.isAlive && !(existing eq assignment) }
      for (duplicate <- duplicates.headOption) {
        errors.rejectValue("name", "name.duplicate.assignment", Array(duplicate.name), "")
      }
    }

    genericValidate(errors)

    // Only validate open date/close date where this has changed, to allow details to still be saved with invalid dates until they've been updated by us
    if (openDate != null && (assignment.openDate == null || !openDate.isEqual(assignment.openDate.toLocalDate))) {
      validateOpenDate(errors)
    }
    if (closeDate != null && !openEnded && (assignment.closeDate == null || !closeDate.isEqual(assignment.closeDate) || !closeDate.toLocalDate.isBefore(Assignment.closeTimeEnforcementDate))) {
      validateCloseDate(errors)
    }

    if (workflowCategory != NoneUse && !assignment.restrictSubmissions) {
      errors.rejectValue("workflowCategory", "markingWorkflow.restrictSubmissions")
    }

    if (openEnded && (assignment.countUnapprovedExtensions > 0 || assignment.approvedExtensions.nonEmpty)) {
      errors.rejectValue("openEnded", "assignment.openEnded.hasExtensions")
    }

    if (assignment.hasWorkflow && !assignment.cm2MarkingWorkflow.canDeleteMarkers) {
      val (existingMarkersA, existingMarkersB) = extractMarkers

      if (!existingMarkersA.forall(markersA.asScala.contains)) {
        errors.rejectValue("markersA", "workflow.cannotRemoveMarkers")
      }

      if (!existingMarkersB.forall(markersB.asScala.contains)) {
        errors.rejectValue("markersB", "workflow.cannotRemoveMarkers")
      }
    }
  }

  // Don't add things here as a Command might mix in multiple validators, add to validateCreateAssignmentDetails
  override def validate(errors: Errors): Unit = validateEditAssignmentDetails(errors)
}

trait EditAssignmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: EditAssignmentDetailsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(notDeleted(assignment), module)
    p.PermissionCheck(Permissions.Assignment.Update, module)
  }
}

trait EditAssignmentDetailsDescription extends Describable[Assignment] {
  self: EditAssignmentDetailsRequest
    with EditAssignmentDetailsCommandState =>

  override lazy val eventName = "EditAssignmentDetails"

  override def describe(d: Description): Unit =
    d.assignment(assignment).properties(
      "name" -> name,
      "openDate" -> Option(openDate).map(_.toString()).orNull,
      "closeDate" -> Option(closeDate).map(_.toString()).orNull,
      "workflowCtg" -> Option(workflowCategory).map(_.code).orNull,
      "workflowType" -> Option(workflowType).map(_.name).orNull,
      "anonymity" -> Option(anonymity).map(_.code).orNull
    )
}
