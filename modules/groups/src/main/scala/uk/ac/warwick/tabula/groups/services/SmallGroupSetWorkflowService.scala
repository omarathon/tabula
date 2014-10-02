package uk.ac.warwick.tabula.groups.services

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{WorkflowProgress, WorkflowStages, WorkflowStage}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet}

@Service
class SmallGroupSetWorkflowService {
	import SmallGroupSetWorkflowStages._

	final val MaxPower = 100

	def getStagesFor(set: SmallGroupSet) = {
		var stages = Seq[SmallGroupSetWorkflowStage]()

		stages = stages ++ Seq(AddGroups, AddStudents, AddEvents)

		if (set.allocationMethod == SmallGroupAllocationMethod.Manual) {
			stages = stages ++ Seq(AllocateStudents)
		} else if (set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp) {
			stages = stages ++ Seq(OpenSignUp, CloseSignUp)
		}

		stages = stages ++ Seq(SendNotifications)

		stages
	}

	def progress(set: SmallGroupSet) = {
		val allStages = getStagesFor(set)
		val progresses = allStages.map { _.progress(set) }

		val workflowMap = WorkflowStages.toMap(progresses)

		// Quick exit for if we're at the end
		if (progresses.last.completed) {
			WorkflowProgress(MaxPower, progresses.last.messageCode, progresses.last.health.cssClass, None, workflowMap)
		} else {
			// get the last started stage
			val stageIndex = progresses.lastIndexWhere(_.started)
			if (stageIndex == -1) WorkflowProgress(0, progresses.head.messageCode, progresses.head.health.cssClass, Some(progresses(0).stage), workflowMap)
			else {
				val lastProgress = progresses(stageIndex)
				val nextProgress = if (lastProgress.completed) progresses(stageIndex + 1) else lastProgress

				val percentage = ((stageIndex + 1) * MaxPower) / allStages.size
				WorkflowProgress(percentage, lastProgress.messageCode, lastProgress.health.cssClass, Some(nextProgress.stage), workflowMap)
			}
		}
	}

}

sealed abstract class SmallGroupSetWorkflowStage extends WorkflowStage {
	def progress(set: SmallGroupSet): WorkflowStages.StageProgress
}

object SmallGroupSetWorkflowStages {
	import WorkflowStages._

	case object AddGroups extends SmallGroupSetWorkflowStage {
		def actionCode = "workflow.smallGroupSet.AddGroups.action"
		def progress(set: SmallGroupSet) =
			// Linked upstream, no groups
			if (set.linkedDepartmentSmallGroupSet != null && set.groups.isEmpty)
				StageProgress(AddGroups, started = true, messageCode = "workflow.smallGroupSet.AddGroups.linkedEmpty", health = Warning)
			else if (set.groups.isEmpty)
				StageProgress(AddGroups, started = false, messageCode = "workflow.smallGroupSet.AddGroups.empty", health = Danger)
			else
				StageProgress(AddGroups, started = true, messageCode = "workflow.smallGroupSet.AddGroups.added", health = Warning, completed = true)
	}

	case object AddStudents extends SmallGroupSetWorkflowStage {
		def actionCode = "workflow.smallGroupSet.AddStudents.action"
		def progress(set: SmallGroupSet) =
			// Linked upstream
			if (set.linked && set.linkedDepartmentSmallGroupSet != null) {
				// Linked upstream (to SITS), no students
				if (set.members.isEmpty && set.linkedDepartmentSmallGroupSet.memberQuery.hasText)
					StageProgress(AddStudents, started = true, messageCode = "workflow.smallGroupSet.AddStudents.linkedUpstreamSits.empty", health = Warning, completed = true)

				// Linked upstream (manually), no students
				else if (set.members.isEmpty && !set.linkedDepartmentSmallGroupSet.memberQuery.hasText)
					StageProgress(AddStudents, started = true, messageCode = "workflow.smallGroupSet.AddStudents.linkedUpstreamManual.empty", health = Warning)

				// Has students
				else
					StageProgress(AddStudents, started = true, messageCode = "workflow.smallGroupSet.AddStudents.hasUpstream", health = Warning, completed = true)
			} else {
				// Linked to SITS, no students
				if (!set.assessmentGroups.isEmpty && set.allStudentsCount == 0)
					StageProgress(AddStudents, started = true, messageCode = "workflow.smallGroupSet.AddStudents.linkedToSits.empty", health = Warning, completed = true)

				// Not linked to SITS, no students
				else if (set.assessmentGroups.isEmpty && set.allStudentsCount == 0)
					StageProgress(AddStudents, started = false, messageCode = "workflow.smallGroupSet.AddStudents.empty", health = Danger)

				// Has students
				else
					StageProgress(AddStudents, started = true, messageCode = "workflow.smallGroupSet.AddStudents.hasStudents", health = Warning, completed = true)
			}
	}

	case object AddEvents extends SmallGroupSetWorkflowStage {
		def actionCode = "workflow.smallGroupSet.AddEvents.action"
		def progress(set: SmallGroupSet) =
			if (set.groups.asScala.forall { _.events.isEmpty })
				StageProgress(AddEvents, started = false, messageCode = "workflow.smallGroupSet.AddEvents.empty", health = Danger)
			else
				StageProgress(AddEvents, started = true, messageCode = "workflow.smallGroupSet.AddEvents.added", health = Warning, completed = true)

		override def preconditions = Seq(Seq(AddGroups))
	}

	case object AllocateStudents extends SmallGroupSetWorkflowStage {
		def actionCode = "workflow.smallGroupSet.AllocateStudents.action"
		def progress(set: SmallGroupSet) =
			if (set.unallocatedStudentsCount == set.allStudentsCount)
				StageProgress(AllocateStudents, started = false, messageCode = "workflow.smallGroupSet.AllocateStudents.none", health = Danger)
			else if (set.unallocatedStudentsCount > 0)
				StageProgress(AllocateStudents, started = true, messageCode = "workflow.smallGroupSet.AllocateStudents.some", health = Warning)
			else
				StageProgress(AllocateStudents, started = true, messageCode = "workflow.smallGroupSet.AllocateStudents.all", health = Warning, completed = true)

		override def preconditions = Seq(Seq(AddGroups, AddStudents))
	}

	case object OpenSignUp extends SmallGroupSetWorkflowStage {
		def actionCode = "workflow.smallGroupSet.OpenSignUp.action"
		def progress(set: SmallGroupSet) =
			if (set.openForSignups)
				StageProgress(OpenSignUp, started = true, messageCode = "workflow.smallGroupSet.OpenSignUp.open", health = Warning, completed = true)
			else if (set.unallocatedStudentsCount == set.allStudentsCount)
				StageProgress(OpenSignUp, started = false, messageCode = "workflow.smallGroupSet.OpenSignUp.notOpen", health = Warning)
			else
				StageProgress(OpenSignUp, started = true, messageCode = "workflow.smallGroupSet.OpenSignUp.partial", health = Warning, completed = true)

		override def preconditions = Seq(Seq(AddGroups, AddStudents))
	}

	case object CloseSignUp extends SmallGroupSetWorkflowStage {
		def actionCode = "workflow.smallGroupSet.CloseSignUp.action"
		def progress(set: SmallGroupSet) =
			if (!set.openForSignups && set.unallocatedStudentsCount < set.allStudentsCount)
				StageProgress(CloseSignUp, started = true, messageCode = "workflow.smallGroupSet.CloseSignUp.closed", health = Warning, completed = true)
			else if (set.openForSignups)
				StageProgress(CloseSignUp, started = false, messageCode = "workflow.smallGroupSet.CloseSignUp.notClosed", health = Warning)
			else
				StageProgress(CloseSignUp, started = false, messageCode = "workflow.smallGroupSet.OpenSignUp.notOpen", health = Warning)

		override def preconditions = Seq(Seq(OpenSignUp))
	}

	case object SendNotifications extends SmallGroupSetWorkflowStage {
		def actionCode = "workflow.smallGroupSet.SendNotifications.action"
		def progress(set: SmallGroupSet) =
			if (set.fullyReleased)
				StageProgress(SendNotifications, started = true, messageCode = "workflow.smallGroupSet.SendNotifications.fullyReleased", health = Good, completed = true)
			else if (set.releasedToStudents)
				StageProgress(SendNotifications, started = true, messageCode = "workflow.smallGroupSet.SendNotifications.releasedToStudents", health = Warning)
			else if (set.releasedToTutors)
				StageProgress(SendNotifications, started = true, messageCode = "workflow.smallGroupSet.SendNotifications.releasedToTutors", health = Warning)
			else
				StageProgress(SendNotifications, started = false, messageCode = "workflow.smallGroupSet.SendNotifications.notSent", health = Warning)

		override def preconditions = Seq(Seq(CloseSignUp), Seq(AllocateStudents), Seq(AddGroups, AddStudents, AddEvents))
	}
}

trait SmallGroupSetWorkflowServiceComponent {
	def smallGroupSetWorkflowService: SmallGroupSetWorkflowService
}

trait AutowiringSmallGroupSetWorkflowServiceComponent extends SmallGroupSetWorkflowServiceComponent {
	var smallGroupSetWorkflowService = Wire[SmallGroupSetWorkflowService]
}