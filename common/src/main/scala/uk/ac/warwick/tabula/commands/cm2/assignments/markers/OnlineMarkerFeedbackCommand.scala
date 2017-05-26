package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.commands.cm2.feedback._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.{AutowiringSavedFormValueDaoComponent, SavedFormValueDaoComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._


object OnlineMarkerFeedbackCommand {
	def apply(assignment: Assignment, stage: MarkingWorkflowStage, student: User, marker: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new OnlineMarkerFeedbackCommandInternal(assignment, stage, student, marker, submitter, gradeGenerator)
			with ComposableCommand[MarkerFeedback]
			with OnlineFeedbackPermissions
			with OnlineFeedbackDescription[MarkerFeedback]
			with AutowiringProfileServiceComponent
			with AutowiringCM2MarkingWorkflowServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringSavedFormValueDaoComponent
			with AutowiringFeedbackServiceComponent
}

class OnlineMarkerFeedbackCommandInternal(
		val assignment: Assignment,
		val stage: MarkingWorkflowStage,
		val student: User,
		val marker: User,
		val submitter: CurrentUser,
		val gradeGenerator: GeneratesGradesFromMarks
	) extends CommandInternal[MarkerFeedback] with OnlineFeedbackCommand with OnlineMarkerFeedbackState with CopyFromFormFields with WriteToFormFields {

	this: ProfileServiceComponent with CM2MarkingWorkflowServiceComponent with FileAttachmentServiceComponent with SavedFormValueDaoComponent
		with FeedbackServiceComponent =>

	currentMarkerFeedback.foreach(copyFrom)

	def applyInternal(): MarkerFeedback = {

		val markerFeedback = currentMarkerFeedback.getOrElse(
			// this should be impossible - we don't show the form if there is no current marker feedback for this marker
			throw new IllegalArgumentException(s"No MarkerFeedback to save for ${marker.getUserId} - ${student.getUserId}")
		)

		copyTo(markerFeedback)
		markerFeedback.uploadedDate = DateTime.now
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
		attachedFiles = markerFeedback.attachments
	}

	def copyTo(markerFeedback: MarkerFeedback) {

		saveFormFields(markerFeedback)

		// save mark and grade
		if (assignment.collectMarks) {
			markerFeedback.mark = mark.maybeText.map(_.toInt)
			markerFeedback.grade = grade.maybeText
		}

		// save attachments
		if (markerFeedback.attachments != null) {
			val filesToKeep =  Option(attachedFiles).getOrElse(JList()).asScala
			val existingFiles = Option(markerFeedback.attachments).getOrElse(JList()).asScala
			val filesToRemove = existingFiles -- filesToKeep
			val filesToReplicate = filesToKeep -- existingFiles
			fileAttachmentService.deleteAttachments(filesToRemove)
			markerFeedback.attachments = JArrayList[FileAttachment](filesToKeep)
			val replicatedFiles = filesToReplicate.map ( _.duplicate() )
			replicatedFiles.foreach(markerFeedback.addAttachment)
		}
		markerFeedback.addAttachments(file.attached.asScala)
	}
}


trait OnlineMarkerFeedbackState extends OnlineFeedbackState with SubmissionState with ExtensionState {

	this: ProfileServiceComponent with CM2MarkingWorkflowServiceComponent =>

	val stage: MarkingWorkflowStage
	val module: Module = assignment.module
	val gradeGenerator: GeneratesGradesFromMarks

	val feedback: AssignmentFeedback = assignment.allFeedback.find(_.usercode == student.getUserId).getOrElse(
		throw new IllegalArgumentException(s"No feedback for ${student.getUserId}")
	)
	val submission: Option[Submission] = assignment.submissions.asScala.find(_.usercode == student.getUserId)
	val extension: Option[Extension] = assignment.extensions.asScala.find(_.usercode == student.getUserId)

	private val allMarkerFeedback = cm2MarkingWorkflowService.markerFeedbackForFeedback(feedback)

	val previousMarkerFeedback: Map[MarkingWorkflowStage, MarkerFeedback] = {
		val currentStageIndex = feedback.currentStageIndex
		if (currentStageIndex <= stage.order)
			allMarkerFeedback.filterKeys(_.order < currentStageIndex) // show all the previous stages
		else
			allMarkerFeedback.filterKeys(_.order <= stage.order) // show all stages up to and including the current one
	}

	val currentMarkerFeedback: Option[MarkerFeedback] = feedback.outstandingStages.asScala
		.flatMap(allMarkerFeedback.get)
		.find(_.marker == marker)
		.filter(_.stage == stage)

}