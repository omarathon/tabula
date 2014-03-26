package uk.ac.warwick.tabula.coursework.services

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.spring.Wire
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.coursework.commands.assignments.WorkflowItems
import scala.collection.immutable.ListMap
import uk.ac.warwick.tabula.data.model.MarkingState.{Rejected, MarkingCompleted}
import uk.ac.warwick.tabula.data.model.MarkingMethod.SeenSecondMarking

@Service
class CourseworkWorkflowService {
	import WorkflowStages._
	
	final val MaxPower = 100
	var features = Wire.auto[Features]
	
	def getStagesFor(assignment: Assignment) = {
		var stages = Seq[WorkflowStage]()
		if (assignment.collectSubmissions) {
			stages = stages ++ Seq(Submission)
			
			if (features.turnitin && assignment.module.department.plagiarismDetectionEnabled) {
				stages = stages ++ Seq(CheckForPlagiarism)
			}
			
			stages = stages ++ Seq(DownloadSubmission)
			
			if (features.markingWorkflows && assignment.markingWorkflow != null) {
				stages = stages ++ Seq(ReleaseForMarking, FirstMarking)
				
				if (assignment.markingWorkflow.hasSecondMarker) {
					stages = stages ++ Seq(SecondMarking)
				}

				if (assignment.markingWorkflow.markingMethod == SeenSecondMarking) {
					stages = stages ++ Seq(FinaliseSeenSecondMarking)
				}
			}
		}
		
		if (assignment.collectMarks) {
			stages = stages ++ Seq(AddMarks)
		}
		
		stages = stages ++ Seq(AddFeedback, ReleaseFeedback, ViewOnlineFeedback, DownloadFeedback)
		
		stages
	}
	
	def progress(assignment: Assignment)(coursework: WorkflowItems) = {
		val allStages = getStagesFor(assignment)
		val progresses = allStages map { _.progress(assignment)(coursework) }
		
		val workflowMap = toMap(progresses)
		
		// Quick exit for if we're at the end
		if (progresses.last.completed) {
			Progress(MaxPower, progresses.last.messageCode, progresses.last.health.cssClass, None, workflowMap)
		} else {
			// get the last started stage
			val stageIndex = progresses.lastIndexWhere(_.started)
			if (stageIndex == -1) Progress(0, progresses.head.messageCode, progresses.head.health.cssClass, None, workflowMap) 
			else {
				val lastProgress = progresses(stageIndex)
				val nextProgress = if (lastProgress.completed) progresses(stageIndex + 1) else lastProgress
				
				val percentage = ((stageIndex + 1) * MaxPower) / allStages.size
				Progress(percentage, lastProgress.messageCode, lastProgress.health.cssClass, Some(nextProgress.stage), workflowMap)
			}
		}
	}
	
	private def toMap(progresses: Seq[WorkflowStages.StageProgress]) = {
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
				messageCode=p.messageCode,
				health=p.health,
				completed=p.completed,
				preconditionsMet=preconditionsMet(p)
			) 
		})
		
		builder.result()
	}
}

case class Progress(
	percentage: Int,
	messageCode: String,
	cssClass: String,
	nextStage: Option[WorkflowStage],
	stages: ListMap[String, WorkflowStages.StageProgress]
)

sealed abstract class WorkflowStage {
	def actionCode: String
	def progress(assignment: Assignment)(coursework: WorkflowItems): WorkflowStages.StageProgress
	
	// Returns a sequence of a sequence of workflows; at least one of the inner sequence must have all been fulfilled.
	// So for an AND, you might just do Seq(Seq(stage1, stage2, stage3)) but for an OR you can do Seq(Seq(stage1), Seq(stage2))
	def preconditions: Seq[Seq[WorkflowStage]] = Seq()
}

sealed abstract class WorkflowStageHealth(val cssClass: String)

object WorkflowStages {
	case class StageProgress(
		stage: WorkflowStage,
		started: Boolean,
		messageCode: String,
		health: WorkflowStageHealth=Good,
		completed: Boolean=false,
		preconditionsMet: Boolean=false
	)
	
	case object Good extends WorkflowStageHealth("success")
	case object Warning extends WorkflowStageHealth("warning")
	case object Danger extends WorkflowStageHealth("danger")
	
	case object Submission extends WorkflowStage {
		def actionCode = "workflow.Submission.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			// If the student hasn't submitted, but we have uploaded feedback for them, don't record their submission status
			case None if coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder).isDefined => StageProgress(Submission, started = false, "workflow.Submission.unsubmitted.withFeedback")
			
			case Some(submission) if submission.submission.isLate => StageProgress(Submission, started = true, "workflow.Submission.late", Warning, completed = true)
			
			case Some(submission) if submission.submission.isAuthorisedLate => StageProgress(Submission, started = true, "workflow.Submission.authorisedLate", Good, completed = true)
			
			case Some(_) => StageProgress(Submission, started = true, "workflow.Submission.onTime", Good, completed = true)
			
			case None if !assignment.isClosed => StageProgress(Submission, started = false, "workflow.Submission.unsubmitted.withinDeadline")
			
			// Not submitted, check extension
			case _ => unsubmittedProgress(assignment)(coursework)
		}
		
		private def unsubmittedProgress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedExtension match {
			case Some(extension) if extension.within => StageProgress(Submission, started = false, "workflow.Submission.unsubmitted.withinExtension")
			
			case _ if assignment.isClosed && !assignment.allowLateSubmissions =>
				StageProgress(Submission, started = true, "workflow.Submission.unsubmitted.failedToSubmit", Danger, completed = false)
			
			case _ => StageProgress(Submission, started = true, "workflow.Submission.unsubmitted.late", Danger, completed = false)
		} 
	}
	
	case object DownloadSubmission extends WorkflowStage {
		def actionCode = "workflow.DownloadSubmission.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			case Some(submission) if submission.downloaded => StageProgress(DownloadSubmission, started = true, "workflow.DownloadSubmission.downloaded", Good, completed = true)
			case Some(_) => StageProgress(DownloadSubmission, started = false, "workflow.DownloadSubmission.notDownloaded")
			case _ => StageProgress(DownloadSubmission, started = false, "workflow.DownloadSubmission.notDownloaded")
		}
		override def preconditions = Seq(Seq(Submission))
	}
	
	case object CheckForPlagiarism extends WorkflowStage {
		def actionCode = "workflow.CheckForPlagiarism.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			case Some(item) if item.submission.suspectPlagiarised =>
				StageProgress(CheckForPlagiarism, started = true, "workflow.CheckForPlagiarism.suspectPlagiarised", Danger, completed = true)
			case Some(item) if item.submission.allAttachments.exists(_.originalityReport != null) =>
				StageProgress(CheckForPlagiarism, started = true, "workflow.CheckForPlagiarism.checked", Good, completed = true)
			case Some(_) => StageProgress(CheckForPlagiarism, started = false, "workflow.CheckForPlagiarism.notChecked")
			case _ => StageProgress(CheckForPlagiarism, started = false, "workflow.CheckForPlagiarism.notChecked")
		}
		override def preconditions = Seq(Seq(Submission))
	}
	
	case object ReleaseForMarking extends WorkflowStage {
		def actionCode = "workflow.ReleaseForMarking.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedSubmission match {
			case Some(item) if item.submission.isReleasedForMarking =>
				StageProgress(ReleaseForMarking, started = true, "workflow.ReleaseForMarking.released", Good, completed = true)
			case Some(_) => StageProgress(ReleaseForMarking, started = false, "workflow.ReleaseForMarking.notReleased")
			case _ => StageProgress(ReleaseForMarking, started = false, "workflow.ReleaseForMarking.notReleased")
		}
		override def preconditions = Seq(Seq(Submission))
	}
	
	case object FirstMarking extends WorkflowStage {
		def actionCode = "workflow.FirstMarking.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedFeedback match {
			case Some(item) =>
				if (item.feedback.retrieveFirstMarkerFeedback.state == MarkingCompleted)
					StageProgress(FirstMarking, started = true, "workflow.FirstMarking.marked", Good, completed = true)
				else
					StageProgress(FirstMarking, started = true, "workflow.FirstMarking.notMarked", Warning, completed = false)
			case _ => StageProgress(FirstMarking, started = false, "workflow.FirstMarking.notMarked")
		}
		override def preconditions = Seq(Seq(Submission, ReleaseForMarking))
	}

	case object SecondMarking extends WorkflowStage {
		def actionCode = "workflow.SecondMarking.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = {
			val hasSubmission = coursework.enhancedSubmission.exists(_.submission.isReleasedToSecondMarker)
			coursework.enhancedFeedback match {
				case Some(item) if hasSubmission &&  item.feedback.retrieveSecondMarkerFeedback.state != Rejected =>
					if (item.feedback.retrieveSecondMarkerFeedback.state == MarkingCompleted)
						StageProgress(SecondMarking, started = true, "workflow.SecondMarking.marked", Good, completed = true)
					else
						StageProgress(SecondMarking, started = true, "workflow.SecondMarking.notMarked", Warning, completed = false)
				case _ => StageProgress(SecondMarking, started = false, "workflow.SecondMarking.notMarked")
			}
		}
		override def preconditions = Seq(Seq(Submission, ReleaseForMarking, FirstMarking))
	}

	case object FinaliseSeenSecondMarking extends WorkflowStage {
		def actionCode = "workflow.FinaliseSeenSecondMarking.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = {
			val hasSubmission = coursework.enhancedSubmission.exists(_.submission.isReleasedToSecondMarker)
			coursework.enhancedFeedback match {
				case Some(item) if hasSubmission &&  item.feedback.retrieveThirdMarkerFeedback.state != Rejected =>
					if (item.feedback.retrieveThirdMarkerFeedback.state == MarkingCompleted )
						StageProgress(FinaliseSeenSecondMarking, started = true, "workflow.FinaliseSeenSecondMarking.finalised", Good, completed = true)
					else
						StageProgress(FinaliseSeenSecondMarking, started = true, "workflow.FinaliseSeenSecondMarking.notFinalised", Warning, completed = false)
				case _ => StageProgress(FinaliseSeenSecondMarking, started = false, "workflow.FinaliseSeenSecondMarking.notFinalised")
			}
		}
		override def preconditions = Seq(Seq(Submission, ReleaseForMarking, FirstMarking, SecondMarking))
	}



	
	case object AddMarks extends WorkflowStage {
		def actionCode = "workflow.AddMarks.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) =
			coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if item.feedback.hasMarkOrGrade =>
					StageProgress(AddMarks, started = true, "workflow.AddMarks.marked", Good, completed = true)
				case Some(_) => StageProgress(AddMarks, started = true, "workflow.AddMarks.notMarked", Warning, completed = false)
				case _ => StageProgress(AddMarks, started = false, "workflow.AddMarks.notMarked")
			}
	}
	
	case object AddFeedback extends WorkflowStage {
		def actionCode = "workflow.AddFeedback.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) = coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
			case Some(item) if item.feedback.hasAttachments || item.feedback.hasOnlineFeedback =>
				StageProgress(AddFeedback, started = true, "workflow.AddFeedback.uploaded", Good, completed = true)
			case Some(_) =>
				StageProgress(AddFeedback, started = true, "workflow.AddFeedback.notUploaded", Warning, completed = false)
			case _ =>
				StageProgress(AddFeedback, started = false, "workflow.AddFeedback.notUploaded")
		}
	}
	
	case object ReleaseFeedback extends WorkflowStage {
		def actionCode = "workflow.ReleaseFeedback.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) =
			coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if item.feedback.released =>
					StageProgress(ReleaseFeedback, started = true, "workflow.ReleaseFeedback.released", Good, completed = true)
				case Some(item) if item.feedback.hasAttachments || item.feedback.hasOnlineFeedback || item.feedback.hasMarkOrGrade =>
					StageProgress(ReleaseFeedback, started = true, "workflow.ReleaseFeedback.notReleased", Warning, completed = false)
				case _ => StageProgress(ReleaseFeedback, started = false, "workflow.ReleaseFeedback.notReleased")
			}
		override def preconditions = Seq(Seq(AddMarks), Seq(AddFeedback))
	}

	case object ViewOnlineFeedback extends WorkflowStage {
		def actionCode = "workflow.ViewOnlineFeedback.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) =
			coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if item.feedback.released && item.onlineViewed =>
					StageProgress(ViewOnlineFeedback, started = true, "workflow.ViewOnlineFeedback.viewed", Good, completed = true)
				case Some(item) if item.feedback.released =>
					StageProgress(ViewOnlineFeedback, started = true, "workflow.ViewOnlineFeedback.notViewed", Warning, completed = false)
				case _ => StageProgress(ViewOnlineFeedback, started = false, "workflow.ViewOnlineFeedback.notViewed")
		}
		override def preconditions = Seq(Seq(ReleaseFeedback))
	}
	
	case object DownloadFeedback extends WorkflowStage {
		def actionCode = "workflow.DownloadFeedback.action"
		def progress(assignment: Assignment)(coursework: WorkflowItems) =
			coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder) match {
				case Some(item) if !(item.onlineViewed && (item.feedback.hasGenericFeedback || item.feedback.hasOnlineFeedback)) && !item.downloaded  =>
					StageProgress(DownloadFeedback, started = false, "workflow.DownloadFeedback.notDownloaded")
				case Some(item) if item.downloaded || !item.feedback.hasAttachments =>
					StageProgress(DownloadFeedback, started = true, "workflow.DownloadFeedback.downloaded", Good, completed = true)
				case Some(item) if item.feedback.released =>
					StageProgress(DownloadFeedback, started = true, "workflow.DownloadFeedback.notDownloaded", Warning, completed = false)
				case _ => StageProgress(DownloadFeedback, started = false, "workflow.DownloadFeedback.notDownloaded")
			}
		override def preconditions = Seq(Seq(ReleaseFeedback, ViewOnlineFeedback), Seq(ReleaseFeedback))
	}
}