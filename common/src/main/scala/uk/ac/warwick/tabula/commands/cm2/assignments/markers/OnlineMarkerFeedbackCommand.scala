package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.feedback._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.{AutowiringSavedFormValueDaoComponent, SavedFormValueDaoComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._
import scala.collection.immutable.SortedMap

object OnlineMarkerFeedbackCommand {
  def apply(assignment: Assignment, stage: MarkingWorkflowStage, student: User, marker: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
    new OnlineMarkerFeedbackCommandInternal(assignment, stage, student, marker, submitter, gradeGenerator)
      with ComposableCommand[MarkerFeedback]
      with OnlineMarkerFeedbackPermissions
      with OnlineFeedbackDescription[MarkerFeedback]
      with OnlineFeedbackValidation
      with OnlineFeedbackBindListener
      with AutowiringProfileServiceComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
      with AutowiringFileAttachmentServiceComponent
      with AutowiringSavedFormValueDaoComponent
      with AutowiringFeedbackServiceComponent {
      override lazy val eventName = "OnlineMarkerFeedback"
    }

}

class IllegalMarkingStateException(message: String) extends IllegalStateException(message)

class OnlineMarkerFeedbackCommandInternal(
  val assignment: Assignment,
  val stage: MarkingWorkflowStage,
  val student: User,
  val marker: User,
  val submitter: CurrentUser,
  val gradeGenerator: GeneratesGradesFromMarks
) extends CommandInternal[MarkerFeedback] with OnlineMarkerFeedbackState with MarkerCopyFromFormFields with MarkerWriteToFormFields {

  self: ProfileServiceComponent with CM2MarkingWorkflowServiceComponent with FileAttachmentServiceComponent with SavedFormValueDaoComponent
    with FeedbackServiceComponent =>

  currentMarkerFeedback.foreach(copyFrom)

  def applyInternal(): MarkerFeedback = transactional() {
    val markerFeedback = currentMarkerFeedback.getOrElse(
      throw new IllegalMarkingStateException("There is no outstanding feedback for this marker and stage")
    )

    copyTo(markerFeedback)
    markerFeedback.updatedOn = DateTime.now
    feedbackService.save(markerFeedback)
    markerFeedback
  }

  def copyFrom(markerFeedback: MarkerFeedback) {
    copyFormFields(markerFeedback)

    // mark and grade
    if (assignment.collectMarks) {
      mark = markerFeedback.mark.map(_.toString).getOrElse("")
      grade = markerFeedback.grade.getOrElse("")
    }

    // get attachments
    attachedFiles = JArrayList[FileAttachment](markerFeedback.attachments.asScala)
  }

  def copyTo(markerFeedback: MarkerFeedback): Unit = {
    saveFormFields(markerFeedback)

    // save mark and grade
    if (assignment.collectMarks) {
      markerFeedback.mark = mark.maybeText.map(_.toInt)
      markerFeedback.grade = grade.maybeText
    }

    // save attachments
    if (markerFeedback.attachments != null) {
      val filesToKeep = Option(attachedFiles).getOrElse(JList()).asScala.toSeq
      val existingFiles = Option(markerFeedback.attachments).getOrElse(JHashSet()).asScala.toSeq
      val filesToRemove = existingFiles diff filesToKeep
      val filesToReplicate = filesToKeep diff existingFiles
      filesToRemove.foreach { f =>
        f.markerFeedback = null
        Option(f.feedback).foreach(_.removeAttachment(f))
      }
      fileAttachmentService.deleteAttachments(filesToRemove)
      markerFeedback.attachments = JHashSet[FileAttachment](filesToKeep: _*)
      val replicatedFiles = filesToReplicate.map(_.duplicate())
      replicatedFiles.foreach(markerFeedback.addAttachment)
    }

    markerFeedback.addAttachments(file.attached.asScala.toSeq)
  }
}

trait MarkerCopyFromFormFields {

  self: OnlineFeedbackState with SavedFormValueDaoComponent =>

  def copyFormFields(markerFeedback: MarkerFeedback) {
    // get custom field values
    fields = {
      val pairs = assignment.feedbackFields.map { field =>
        val currentValue = markerFeedback.customFormValues.asScala.find(_.name == field.name)
        val formValue = currentValue match {
          case Some(initialValue) => field.populatedFormValue(initialValue)
          case None => field.blankFormValue
        }
        field.id -> formValue
      }
      Map(pairs: _*).asJava
    }
  }

}

trait MarkerWriteToFormFields {

  self: OnlineFeedbackState with SavedFormValueDaoComponent =>

  def saveFormFields(markerFeedback: MarkerFeedback) {
    // save custom fields
    markerFeedback.customFormValues = fields.asScala.map {
      case (_, formValue) =>

        def newValue = {
          val newValue = new SavedFormValue()
          newValue.name = formValue.field.name
          newValue.markerFeedback = markerFeedback
          newValue
        }

        // Don't send brand new feedback to the DAO or we'll get a TransientObjectException
        val savedFormValue = if (markerFeedback.id == null) {
          newValue
        } else {
          savedFormValueDao.get(formValue.field, markerFeedback).getOrElse(newValue)
        }

        formValue.persist(savedFormValue)
        savedFormValue
    }.toSet[SavedFormValue].asJava
  }

}

trait OnlineMarkerFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: OnlineMarkerFeedbackState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, mandatory(assignment))
    if (submitter.apparentUser != marker) {
      p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
    }
  }
}

trait OnlineMarkerFeedbackState extends OnlineFeedbackState {

  self: ProfileServiceComponent with CM2MarkingWorkflowServiceComponent =>

  val marker: User
  val stage: MarkingWorkflowStage
  val gradeGenerator: GeneratesGradesFromMarks

  private val allMarkerFeedback = feedback.map(_.feedbackByStage).getOrElse(SortedMap[MarkingWorkflowStage, MarkerFeedback]())

  val previousMarkerFeedback: Map[MarkingWorkflowStage, MarkerFeedback] = {
    val currentStageIndex = feedback.map(_.currentStageIndex).getOrElse(0)
    if (currentStageIndex <= stage.order)
      allMarkerFeedback.filter {
        // show all the previous stages, or this marker's completed feedback
        case (workflowStage, markerFeedback) => workflowStage.order < currentStageIndex || (markerFeedback.marker == marker && feedback.exists(_.isMarkedByStage(workflowStage)))
      }
    else
      allMarkerFeedback.view.filterKeys(_.order <= stage.order).toMap // show all stages up to and including the current one
  }

  val currentMarkerFeedback: Option[MarkerFeedback] = feedback.flatMap(
    _.outstandingStages.asScala
      .flatMap(allMarkerFeedback.get)
      .find(_.marker == marker)
      .filter(_.stage == stage)
  )

}
