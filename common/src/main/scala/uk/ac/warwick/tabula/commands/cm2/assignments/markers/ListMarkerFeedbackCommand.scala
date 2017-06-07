package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{CurrentUser, WorkflowStages}
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.ListMarkerFeedbackCommand.EnhancedFeedbackByStage
import uk.ac.warwick.tabula.commands.cm2.{CommandWorkflowStudentsForAssignment, WorkflowStudentsForAssignment}
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.helpers.cm2.SubmissionAndFeedbackInfoFilters.OverlapPlagiarismFilter
import uk.ac.warwick.tabula.helpers.cm2.{AssignmentSubmissionStudentInfo, SubmissionAndFeedbackInfoFilter, SubmissionAndFeedbackInfoMarkerFilter, WorkflowItems}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.cm2.{AutowiringCM2WorkflowProgressServiceComponent, CM2WorkflowProgressServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap

case class EnhancedMarkerFeedback(
	markerFeedback: MarkerFeedback,
	workflowStudent: MarkingWorkflowStudent
)

case class MarkingWorkflowStudent (
	stages: Seq[WorkflowStages.StageProgress],
	info: AssignmentSubmissionStudentInfo
) {
	def coursework: WorkflowItems = info.coursework
	def assignment: Assignment = info.assignment
	def nextAction: Option[String] = stages.filterNot(_.completed).headOption.map(_.stage.actionCode)
}

object ListMarkerFeedbackCommand {

	type EnhancedFeedbackByStage = SortedMap[MarkingWorkflowStage, Seq[EnhancedMarkerFeedback]]

	def apply(assignment:Assignment, marker:User, submitter: CurrentUser) = new ListMarkerFeedbackCommandInternal(assignment, marker, submitter)
		with ComposableCommand[EnhancedFeedbackByStage]
		with ListMarkerFeedbackPermissions
		with AutowiringCM2MarkingWorkflowServiceComponent
		with AutowiringCM2WorkflowProgressServiceComponent
		with MarkerProgress
		with CommandWorkflowStudentsForAssignment
		with Unaudited with ReadOnly
}

class ListMarkerFeedbackCommandInternal(val assignment:Assignment, val marker:User, val submitter: CurrentUser) extends CommandInternal[EnhancedFeedbackByStage]
	with ListMarkerFeedbackState {

	self: CM2MarkingWorkflowServiceComponent with CM2WorkflowProgressServiceComponent with MarkerProgress =>

	def applyInternal(): EnhancedFeedbackByStage = {
		val enhancedFeedbackByStage = enhance(assignment, cm2MarkingWorkflowService.getAllFeedbackForMarker(assignment, marker))
		enhancedFeedbackByStage.map{ case (stage, feedback) =>
			val filtered = benchmarkTask(s"Do marker feedback filtering for ${stage.name}") { feedback.filter { emf =>
				val info = emf.workflowStudent.info
				val itemExistsInPlagiarismFilters = plagiarismFilters.asScala.isEmpty || plagiarismFilters.asScala.exists(_.predicate(info))
				val itemExistsInSubmissionStatesFilters = submissionStatesFilters.asScala.isEmpty || submissionStatesFilters.asScala.exists(_.predicate(info))
				val itemExistsInMarkerStatusesFilters = markerStateFilters.asScala.isEmpty || markerStateFilters.asScala.exists(_.predicate(info, marker))
				itemExistsInPlagiarismFilters && itemExistsInSubmissionStatesFilters && itemExistsInMarkerStatusesFilters
			}}
			stage -> filtered
		}
	}
}

trait ListMarkerFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListMarkerFeedbackState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait ListMarkerFeedbackState extends CanProxy {
	val assignment: Assignment
	val marker: User
	val submitter: CurrentUser

	var plagiarismFilters: JList[SubmissionAndFeedbackInfoFilter] = JArrayList()
	var submissionStatesFilters: JList[SubmissionAndFeedbackInfoFilter] = JArrayList()
	var markerStateFilters: JList[SubmissionAndFeedbackInfoMarkerFilter] = JArrayList()
	var overlapFilter: OverlapPlagiarismFilter = new OverlapPlagiarismFilter
}

trait CanProxy {

	val marker: User
	val submitter: CurrentUser

	def isProxying: Boolean = marker != submitter.apparentUser
}

trait MarkerProgress extends TaskBenchmarking {

	self: WorkflowStudentsForAssignment with CM2WorkflowProgressServiceComponent =>

	type FeedbackByStage = SortedMap[MarkingWorkflowStage, Seq[MarkerFeedback]]
	type EnhancedFeedbackByStage = SortedMap[MarkingWorkflowStage, Seq[EnhancedMarkerFeedback]]

	protected def enhance(assignment: Assignment, feedbackByStage: FeedbackByStage): EnhancedFeedbackByStage = benchmarkTask(s"Get workflow progress information for ${assignment.name}") {
		val allMarkingStages = workflowProgressService.getStagesFor(assignment).filter(_.markingRelated)
		val workflowStudents = workflowStudentsFor(assignment)

		feedbackByStage.mapValues(mfs => mfs.flatMap(mf => {
			workflowStudents.find(_.user == mf.student).map(ws => {
				val markingStages = allMarkingStages.flatMap(ms => ws.stages.get(ms.toString))
				EnhancedMarkerFeedback(mf, MarkingWorkflowStudent(markingStages, ws))
			})
		}))
	}
}