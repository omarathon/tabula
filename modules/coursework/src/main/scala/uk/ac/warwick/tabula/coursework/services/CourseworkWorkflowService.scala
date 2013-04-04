package uk.ac.warwick.tabula.coursework.services

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.MarkingMethod
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.coursework.commands.assignments.WorkflowItems
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.MarkingState
import scala.collection.immutable.ListMap

@Service
class CourseworkWorkflowService {
	import WorkflowStages._
	
	var features = Wire.auto[Features]
	
	def getStagesFor(assignment: Assignment) = {
		var stages = Seq[WorkflowStage]()
		if (assignment.collectSubmissions) {
			stages = stages ++ Seq(Submission, DownloadSubmission)
			
			if (features.turnitin) {
				stages = stages ++ Seq(CheckForPlagiarism)
			}
			
			if (features.markingWorkflows && assignment.markingWorkflow != null) {
				stages = stages ++ Seq(ReleaseForMarking, FirstMarking)
				
				if (assignment.markingWorkflow.markingMethod == MarkingMethod.SeenSecondMarking) {
					stages = stages ++ Seq(SecondMarking)
				}
			}
		}
		
		if (assignment.collectMarks) {
			stages = stages ++ Seq(AddMarks)
		}
		
		stages = stages ++ Seq(AddFeedback, ReleaseFeedback, DownloadFeedback)
		
		stages
	}
	
	def progress(assignment: Assignment, user: User)(coursework: WorkflowItems) = {
		val allStages = getStagesFor(assignment)
		val progresses = allStages map { _.progress(assignment)(coursework) }
		
		def message(lastProgress: WorkflowStages.StageProgress) =
			progresses.find(_.stage == Submission) match {
				case Some(progress) if lastProgress.stage != Submission && progress.message.hasText =>
					progress.message + ". " + lastProgress.message
				case _ => lastProgress.message
			}
		
		val builder = ListMap.newBuilder[String, WorkflowStages.StageProgress]
		
		def preconditionsMet(p: WorkflowStages.StageProgress) = 
			if (p.stage.preconditions.isEmpty) true
			// For each item in at least one predicate, we have completed
			else p.stage.preconditions.exists { predicate => predicate.forall { stage =>
				progresses.find(_.stage == stage) match {
					case Some(progress) if progress.completed => true
					case _ => false
				}
			}}
		
		// We know at this point whether all the preconditions have been met
		builder ++= (progresses map { p => 
			p.stage.toString -> WorkflowStages.StageProgress(
				stage=p.stage,
				started=p.started,
				message=p.message,
				health=p.health,
				completed=p.completed,
				preconditionsMet=preconditionsMet(p)
			) 
		})
		
		val workflowMap = builder.result
		
		// Quick exit for if we're at the end
		if (progresses.last.completed) {
			Progress(100, message(progresses.last), progresses.last.health.cssClass, None, workflowMap)
		} else {
			// get the last started stage
			val stageIndex = progresses.lastIndexWhere(_.started)
			if (stageIndex == -1) Progress(0, progresses.head.message, progresses.head.health.cssClass, None, workflowMap) 
			else {
				val lastProgress = progresses(stageIndex)
				val nextProgress = if (lastProgress.completed) progresses(stageIndex + 1) else lastProgress
				
				val percentage = ((stageIndex + 1) * 100) / allStages.size
				Progress(percentage, message(lastProgress), lastProgress.health.cssClass, Some(nextProgress.stage), workflowMap)
			}
		}
	}
}

case class Progress(
	val percentage: Int,
	val message: String,
	val cssClass: String,
	val nextStage: Option[WorkflowStage],
	val stages: ListMap[String, WorkflowStages.StageProgress]
)

sealed abstract class WorkflowStage {
	def action: String
	def progress(assignment: Assignment)(coursework: WorkflowItems): WorkflowStages.StageProgress
	
	// Returns a sequence of a sequence of workflows; at least one of the inner sequence must have all been fulfilled.
	// So for an AND, you might just do Seq(Seq(stage1, stage2, stage3)) but for an OR you can do Seq(Seq(stage1), Seq(stage2))
	def preconditions: Seq[Seq[WorkflowStage]] = Seq()
}

sealed abstract class WorkflowStageHealth(val cssClass: String)

object WorkflowStages {
	case class StageProgress(
		val stage: WorkflowStage,
		val started: Boolean,
		val message: String,
		val health: WorkflowStageHealth=Good,
		val completed: Boolean=false,
		val preconditionsMet: Boolean=false
	)
	
	case object Good extends WorkflowStageHealth("success")
	case object Warning extends WorkflowStageHealth("warning")
	case object Danger extends WorkflowStageHealth("danger")
	
	case object Submission extends WorkflowStage {
		def action = "Assignment needs submitting"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			// If the student hasn't submitted, but we have uploaded feedback for them, don't record their submission status
			case None if coursework.enhancedFeedback.isDefined => StageProgress(Submission, false, "Not submitted")
			
			case Some(submission) if submission.submission.isLate => StageProgress(Submission, true, "Submitted late", Warning, true)
			
			case Some(submission) if submission.submission.isAuthorisedLate => StageProgress(Submission, true, "Submitted within extension", Good, true)
			
			case Some(_) => StageProgress(Submission, true, "Submitted on time", Good, true)
			
			case None if !assignment.isClosed => StageProgress(Submission, false, "Not submitted, within deadline")
			
			// Not submitted
			case _ => coursework.enhancedExtension match {
				case Some(extension) if extension.within => StageProgress(Submission, false, "Not submitted, within extension")
				
				case _ => StageProgress(Submission, true, "Not submitted", Danger, false)
			}
		}
	}
	
	case object DownloadSubmission extends WorkflowStage {
		def action = "Submission needs downloading"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			case Some(submission) if submission.downloaded => StageProgress(DownloadSubmission, true, "Submission downloaded", Good, true)
			case Some(_) => StageProgress(DownloadSubmission, false, "Submission not downloaded")
			case _ => StageProgress(DownloadSubmission, false, "Submission not downloaded")
		}
		override def preconditions = Seq(Seq(Submission))
	}
	
	case object CheckForPlagiarism extends WorkflowStage {
		def action = "Submission needs checking for plagiarism"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			case Some(item) if item.submission.suspectPlagiarised =>
				StageProgress(CheckForPlagiarism, true, "Marked as plagiarised", Danger, true)
			case Some(item) if item.submission.allAttachments.find(_.originalityReport != null).isDefined => 
				StageProgress(CheckForPlagiarism, true, "Checked and not plagiarised", Good, true)
			case Some(_) => StageProgress(CheckForPlagiarism, false, "Not checked for plagiarism")
			case _ => StageProgress(CheckForPlagiarism, false, "Not checked for plagiarism")
		}
		override def preconditions = Seq(Seq(Submission))
	}
	
	case object ReleaseForMarking extends WorkflowStage {
		def action = "Submission needs to be released for marking"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			case Some(item) if item.submission.isReleasedForMarking =>
				StageProgress(ReleaseForMarking, true, "Released for marking", Good, true)
			case Some(_) => StageProgress(ReleaseForMarking, false, "Not released for marking")
			case _ => StageProgress(ReleaseForMarking, false, "Not released for marking")
		}
		override def preconditions = Seq(Seq(Submission))
	}
	
	case object FirstMarking extends WorkflowStage {
		def action = "Submission needs marking by [FIRST_MARKER]"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			case Some(item) if item.submission.isReleasedForMarking => {
				val releasedToSecondMarker = item.submission.isReleasedToSecondMarker
				val markingCompleted = item.submission.state == MarkingState.MarkingCompleted
				
				if (releasedToSecondMarker || markingCompleted)
					StageProgress(FirstMarking, true, "Marked by first marker", Good, true)
				else
					StageProgress(FirstMarking, true, "Not marked by first marker", Warning, false)
			}
			case _ => StageProgress(FirstMarking, false, "Not marked by first marker")
		}
		override def preconditions = Seq(Seq(Submission, ReleaseForMarking))
	}
	
	case object SecondMarking extends WorkflowStage {
		def action = "Submission needs marking by [SECOND_MARKER]"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			case Some(item) if item.submission.isReleasedToSecondMarker => {
				val markingCompleted = item.submission.state == MarkingState.MarkingCompleted
				
				if (markingCompleted)
					StageProgress(SecondMarking, true, "Marked by second marker", Good, true)
				else
					StageProgress(SecondMarking, true, "Not marked by second marker", Warning, false)
			}
			case _ => StageProgress(SecondMarking, false, "Not marked by second marker")
		}
		override def preconditions = Seq(Seq(Submission, ReleaseForMarking, FirstMarking))
	}
	
	case object AddMarks extends WorkflowStage {
		def action = "Marks need adding"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedFeedback match {
			case Some(item) if item.feedback.hasMarkOrGrade =>
				StageProgress(AddMarks, true, "Marked", Good, true)
			case Some(_) => StageProgress(AddMarks, true, "Marks not added", Warning, false)
			case _ => StageProgress(AddMarks, false, "Marks not added")
		}
	}
	
	case object AddFeedback extends WorkflowStage {
		def action = "Feedback needs uploading"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedFeedback match {
			case Some(item) if item.feedback.hasAttachments =>
				StageProgress(AddFeedback, true, "Feedback uploaded", Good, true)
			case Some(_) => StageProgress(AddFeedback, true, "Feedback not uploaded", Warning, false)
			case _ => StageProgress(AddFeedback, false, "Feedback not uploaded")
		}
	}
	
	case object ReleaseFeedback extends WorkflowStage {
		def action = "Feedback needs publishing"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedFeedback match {
			case Some(item) if item.feedback.released =>
				StageProgress(ReleaseFeedback, true, "Feedback published", Good, true)
			case Some(item) if item.feedback.hasAttachments => 
				StageProgress(ReleaseFeedback, true, "Feedback not published", Warning, false)
			case _ => StageProgress(ReleaseFeedback, false, "Feedback not published")
		}
		override def preconditions = Seq(Seq(AddMarks), Seq(AddFeedback))
	}
	
	case object DownloadFeedback extends WorkflowStage {
		def action = "Feedback needs downloading by [STUDENT]"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedFeedback match {
			case Some(item) if item.downloaded =>
				StageProgress(DownloadFeedback, true, "Feedback downloaded by student", Good, true)
			case Some(item) if item.feedback.released =>
				StageProgress(DownloadFeedback, true, "Feedback not downloaded by student", Warning, false)
			case _ => StageProgress(DownloadFeedback, false, "Feedback not downloaded by student")
		}
		override def preconditions = Seq(Seq(ReleaseFeedback))
	}
}