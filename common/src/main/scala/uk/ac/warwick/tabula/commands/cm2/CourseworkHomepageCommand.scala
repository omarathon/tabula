package uk.ac.warwick.tabula.commands.cm2

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.WorkflowStages.StageProgress
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.CourseworkHomepageCommand._
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.cm2.AssignmentSubmissionStudentInfo
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.CM2WorkflowStages.{CM1ReleaseForMarking, CM2ReleaseForMarking}
import uk.ac.warwick.tabula.services.cm2.{AutowiringCM2WorkflowProgressServiceComponent, CM2WorkflowProgressServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, WorkflowStage}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object CourseworkHomepageCommand {
	case class StudentAssignmentInformation(
		assignment: Assignment,
		submission: Option[Submission],
		extension: Option[Extension],
		extended: Boolean,
		hasActiveExtension: Boolean,
		extensionRequested: Boolean,
		studentDeadline: DateTime,
		submittable: Boolean,
		resubmittable: Boolean,
		feedback: Option[AssignmentFeedback],
		feedbackDeadline: Option[LocalDate],
		feedbackLate: Boolean
	)

	case class CourseworkHomepageStudentInformation(
		upcomingAssignments: Seq[StudentAssignmentInformation],
		actionRequiredAssignments: Seq[StudentAssignmentInformation],
		noActionRequiredAssignments: Seq[StudentAssignmentInformation],
		completedAssignments: Seq[StudentAssignmentInformation]
	) {
		def isEmpty: Boolean = upcomingAssignments.isEmpty && actionRequiredAssignments.isEmpty && noActionRequiredAssignments.isEmpty && completedAssignments.isEmpty
		def nonempty: Boolean = !isEmpty
	}

	case class MarkingStage(
		stage: WorkflowStage,
		progress: Seq[MarkingStageProgress]
	) {
		def started: Boolean = progress.exists(_.progress.started)
		def completed: Boolean = progress.forall(_.progress.completed)
	}

	case class MarkingStageProgress(
		progress: StageProgress,
		count: Int
	)

	case class MarkingNextStage(
		stage: WorkflowStage,
		count: Int
	)

	case class MarkerAssignmentInfo(
		assignment: Assignment,
		feedbackDeadline: Option[LocalDate],
		extensionCount: Int,
		unsubmittedCount: Int,
		lateSubmissionsCount: Int,
		currentStages: Seq[MarkingStage],
		stages: Seq[MarkingStage],
		nextStages: Seq[MarkingNextStage]
	)

	case class CourseworkHomepageMarkerInformation(
		upcomingAssignments: Seq[MarkerAssignmentInfo],
		actionRequiredAssignments: Seq[MarkerAssignmentInfo],
		noActionRequiredAssignments: Seq[MarkerAssignmentInfo],
		completedAssignments: Seq[MarkerAssignmentInfo]
	) {
		def isEmpty: Boolean = upcomingAssignments.isEmpty && actionRequiredAssignments.isEmpty && noActionRequiredAssignments.isEmpty && completedAssignments.isEmpty
		def nonempty: Boolean = !isEmpty
	}

	case class CourseworkHomepageAdminInformation(
		moduleManagerDepartments: Seq[Department],
		adminDepartments: Seq[Department]
	) {
		def isEmpty: Boolean = moduleManagerDepartments.isEmpty && adminDepartments.isEmpty
		def nonempty: Boolean = !isEmpty
	}

	case class CourseworkHomepageInformation(
		homeDepartment: Option[Department],
		studentInformation: CourseworkHomepageStudentInformation,
		markerInformation: CourseworkHomepageMarkerInformation,
		adminInformation: CourseworkHomepageAdminInformation
	)

	type Result = CourseworkHomepageInformation
	type Command = Appliable[Result] with CourseworkHomepageCommandState

	val AdminPermission = Permissions.Module.ManageAssignments

	def apply(academicYear: AcademicYear, user: CurrentUser): Command =
		new CourseworkHomepageCommandInternal(academicYear, user)
			with ComposableCommand[Result]
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringAssessmentServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringCM2MarkingWorkflowServiceComponent
			with MarkerProgress
			with CommandWorkflowStudentsForAssignment
			with AutowiringCM2WorkflowProgressServiceComponent
			with PubliclyVisiblePermissions with Unaudited with ReadOnly
}

trait CourseworkHomepageCommandState {
	def academicYear: AcademicYear
	def user: CurrentUser
}

class CourseworkHomepageCommandInternal(val academicYear: AcademicYear, val user: CurrentUser) extends CommandInternal[Result]
	with CourseworkHomepageCommandState
	with CourseworkHomepageHomeDepartment
	with CourseworkHomepageStudentAssignments
	with CourseworkHomepageMarkerAssignments
	with CourseworkHomepageAdminDepartments
	with TaskBenchmarking {
	self: ModuleAndDepartmentServiceComponent
		with AssessmentServiceComponent
		with AssessmentMembershipServiceComponent
		with MarkerProgress
		with CM2MarkingWorkflowServiceComponent =>

	override def applyInternal(): Result =
		CourseworkHomepageInformation(
			homeDepartment,
			studentInformation,
			markerInformation,
			adminInformation
		)

}

trait CourseworkHomepageHomeDepartment extends TaskBenchmarking {
	self: CourseworkHomepageCommandState
		with ModuleAndDepartmentServiceComponent =>

	lazy val homeDepartment: Option[Department] = benchmarkTask("Get user's home department") {
		user.departmentCode.maybeText.flatMap(moduleAndDepartmentService.getDepartmentByCode)
	}
}

trait CourseworkHomepageAdminDepartments extends TaskBenchmarking {
	self: CourseworkHomepageCommandState
		with ModuleAndDepartmentServiceComponent =>

	lazy val adminInformation: CourseworkHomepageAdminInformation = benchmarkTask("Get admin information") {
		CourseworkHomepageAdminInformation(
			moduleManagerDepartments,
			adminDepartments
		)
	}

	lazy val moduleManagerDepartments: Seq[Department] = benchmarkTask("Get module manager departments") {
		val ownedModules = benchmarkTask("Get owned modules") {
			moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments)
		}

		ownedModules.map(_.adminDepartment).toSeq.sortBy(_.name)
	}

	lazy val adminDepartments: Seq[Department] = benchmarkTask("Get admin departments") {
		val ownedDepartments = benchmarkTask("Get owned departments") {
			moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.ManageAssignments)
		}

		ownedDepartments.toSeq.sortBy(_.name)
	}

}

trait CourseworkHomepageStudentAssignments extends TaskBenchmarking {
	self: CourseworkHomepageCommandState
		with AssessmentServiceComponent
		with AssessmentMembershipServiceComponent =>

	lazy val studentInformation: CourseworkHomepageStudentInformation = benchmarkTask("Get student information") {
		CourseworkHomepageStudentInformation(
			studentUpcomingAssignments,
			studentActionRequiredAssignments,
			studentNoActionRequiredAssignments,
			studentCompletedAssignments
		)
	}

	private lazy val assignmentsWithFeedback = benchmarkTask("Get assignments with feedback") {
		assessmentService.getAssignmentsWithFeedback(user.userId, Some(academicYear))
	}

	private lazy val assignmentsWithSubmission = benchmarkTask("Get assignments with submission") {
		assessmentService.getAssignmentsWithSubmission(user.userId, Some(academicYear))
	}

	private lazy val enrolledAssignments = benchmarkTask("Get enrolled assignments") {
		assessmentMembershipService.getEnrolledAssignments(user.apparentUser, Some(academicYear))
	}

	private def lateFormative(assignment: Assignment) = !assignment.summative && assignment.isClosed

	// Public for testing
	def enhance(assignment: Assignment): StudentAssignmentInformation = {
		val extension = assignment.extensions.asScala.find(e => e.isForUser(user.apparentUser))
		// isExtended: is within an approved extension
		val isExtended = assignment.isWithinExtension(user.apparentUser)
		// hasActiveExtension: active = approved
		val hasActiveExtension = extension.exists(_.approved)
		val extensionRequested = extension.isDefined && !extension.get.isManual
		val submission = assignment.submissions.asScala.find(_.isForUser(user.apparentUser))
		val feedback = assignment.feedbacks.asScala.filter(_.released).find(_.isForUser(user.apparentUser))
		val feedbackDeadline = submission.flatMap(assignment.feedbackDeadlineForSubmission).orElse(assignment.feedbackDeadline)

		StudentAssignmentInformation(
			assignment = assignment,
			submission = submission,
			extension = extension,
			extended = isExtended,
			hasActiveExtension = hasActiveExtension,
			extensionRequested = extensionRequested,
			studentDeadline = assignment.submissionDeadline(user.apparentUser),
			submittable = assignment.submittable(user.apparentUser),
			resubmittable = assignment.resubmittable(user.apparentUser),
			feedback = feedback,
			feedbackDeadline = feedbackDeadline,
			feedbackLate = feedbackDeadline.exists(_.isBefore(LocalDate.now))
		)
	}

	private def hasEarlierPersonalDeadline(ass1: Assignment, ass2: Assignment): Boolean = {
		// TAB-569 personal time to deadline - if ass1 is "due" before ass2 for the current user
		// Show open ended assignments after
		if (ass2.openEnded && !ass1.openEnded) true
		else if (ass1.openEnded && !ass2.openEnded) false
		else {
			def timeToDeadline(ass: Assignment) = {
				val extension = ass.extensions.asScala.find(e => e.isForUser(user.apparentUser))
				val isExtended = ass.isWithinExtension(user.apparentUser)

				if (ass.openEnded) ass.openDate
				else if (isExtended) extension.flatMap(_.expiryDate).getOrElse(ass.closeDate)
				else ass.closeDate
			}

			timeToDeadline(ass1) < timeToDeadline(ass2)
		}
	}

	private lazy val allUnsubmittedAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get un-submitted assignments") {
		enrolledAssignments
			.diff(assignmentsWithFeedback)
			.diff(assignmentsWithSubmission)
			.filter(_.collectSubmissions) // TAB-475
			.filterNot(lateFormative)
			.sortWith(hasEarlierPersonalDeadline)
			.map(enhance)
	}

	private lazy val studentUpcomingAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get upcoming assignments") {
		allUnsubmittedAssignments.filterNot(_.assignment.isOpened)
	}

	private lazy val studentActionRequiredAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get action required assignments") {
		allUnsubmittedAssignments.diff(studentUpcomingAssignments)
	}

	private def hasEarlierEffectiveDate(ass1: StudentAssignmentInformation, ass2: StudentAssignmentInformation): Boolean = {
		def effectiveDate(info: StudentAssignmentInformation) =
			info.submission.map(_.submittedDate).getOrElse {
				val assignment = info.assignment
				if (assignment.openEnded) assignment.openDate
				else assignment.closeDate
			}

		effectiveDate(ass1) < effectiveDate(ass2)
	}

	lazy val studentNoActionRequiredAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get in-progress assignments") {
		assignmentsWithSubmission
			.diff(assignmentsWithFeedback)
			.map(enhance)
			.sortWith(hasEarlierEffectiveDate)
	}

	lazy val studentCompletedAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get past assignments") {
		(assignmentsWithFeedback ++ enrolledAssignments.filter(lateFormative))
			.map(enhance)
			.sortWith(hasEarlierEffectiveDate)
	}

}

trait CourseworkHomepageMarkerAssignments extends TaskBenchmarking {
	self: CourseworkHomepageCommandState
		with AssessmentServiceComponent
		with CM2MarkingWorkflowServiceComponent
		with MarkerProgress =>

	lazy val markerInformation: CourseworkHomepageMarkerInformation = benchmarkTask("Get marker information") {
		CourseworkHomepageMarkerInformation(
			markerUpcomingAssignments,
			markerActionRequiredAssignments,
			markerNoActionRequiredAssignments,
			markerCompletedAssignments
		)
	}

	// Upcoming - assignments involving marker but not yet released
	private lazy val markerUpcomingAssignments: Seq[MarkerAssignmentInfo] = benchmarkTask("Get upcoming assignments") {
		allMarkerAssignments.filterNot { info =>
			info.stages
				.filter { s => s.stage == CM1ReleaseForMarking || s.stage == CM2ReleaseForMarking }
				.exists(_.progress.exists(_.progress.completed))
		}
	}

	// Action required - assignments which need an action
	private lazy val markerActionRequiredAssignments: Seq[MarkerAssignmentInfo] = benchmarkTask("Get action required assignments") {
		allMarkerAssignments.diff(markerUpcomingAssignments).diff(markerCompletedAssignments).filter { info =>
			info.nextStages.nonEmpty
		}
	}

	// No action required - Assignments that are in the workflow but aren't in need of an action at this stage
	private lazy val markerNoActionRequiredAssignments: Seq[MarkerAssignmentInfo] = benchmarkTask("Get no action required assignments") {
		allMarkerAssignments.diff(markerUpcomingAssignments).diff(markerCompletedAssignments).diff(markerActionRequiredAssignments)
	}

	// Completed - Assignments finished
	private lazy val markerCompletedAssignments: Seq[MarkerAssignmentInfo] = benchmarkTask("Get completed assignments") {
		allMarkerAssignments.filter { info =>
			info.stages.nonEmpty && info.stages.last.progress.forall(_.progress.completed)
		}
	}

	private lazy val allCM1MarkerAssignments: Seq[MarkerAssignmentInfo] = benchmarkTask("Get CM1 assignments for marking") {
		assessmentService.getAssignmentWhereMarker(user.apparentUser, Some(academicYear))
			.map { assignment =>
				val markingWorkflow = assignment.markingWorkflow
				val students = markingWorkflow.getMarkersStudents(assignment, user.apparentUser).toSet

				enhance(assignment, students)
			}
	}

	private lazy val allCM2MarkerAssignments: Seq[MarkerAssignmentInfo] = benchmarkTask("Get CM2 assignments for marking") {
		assessmentService.getCM2AssignmentsWhereMarker(user.apparentUser, Some(academicYear))
			.map { assignment =>
				val markerFeedbacks = cm2MarkingWorkflowService.getAllFeedbackForMarker(assignment, user.apparentUser).values.flatten
				val students = markerFeedbacks.map(_.student).toSet

				enhance(assignment, students)
			}
	}

	private lazy val allMarkerAssignments: Seq[MarkerAssignmentInfo] = benchmarkTask("Get assignments for marking") {
		(allCM1MarkerAssignments ++ allCM2MarkerAssignments)
			.sortBy(info => (info.assignment.openEnded, info.assignment.closeDate))
	}

}

trait WorkflowStudentsForAssignment {
	def workflowStudentsFor(assignment: Assignment): Seq[AssignmentSubmissionStudentInfo]
}

trait CommandWorkflowStudentsForAssignment extends WorkflowStudentsForAssignment {
	def workflowStudentsFor(assignment: Assignment): Seq[AssignmentSubmissionStudentInfo] = SubmissionAndFeedbackCommand(assignment).apply().students
}

trait MarkerProgress extends TaskBenchmarking {
	self: WorkflowStudentsForAssignment
		with CM2WorkflowProgressServiceComponent =>

	protected def enhance(assignment: Assignment, students: Set[User]): MarkerAssignmentInfo = benchmarkTask(s"Get progress information for ${assignment.name}") {
		val workflowStudents = workflowStudentsFor(assignment).filter(s => students.contains(s.user))

		val allStages = workflowProgressService.getStagesFor(assignment).filter(_.markingRelated)

		val currentStages = allStages.flatMap { stage =>
			val name = stage.toString

			val progresses = workflowStudents.flatMap(_.stages.get(name)).filter(_.preconditionsMet)
			if (progresses.nonEmpty) {
				Seq(MarkingStage(
					stage,
					progresses.groupBy(identity).mapValues(_.size).toSeq.map { case (p, c) => MarkingStageProgress(p, c) }
				))
			} else {
				Nil
			}
		}

		val stages = allStages.flatMap { stage =>
			val name = stage.toString

			val progresses = workflowStudents.flatMap(_.stages.get(name))
			if (progresses.nonEmpty) {
				Seq(MarkingStage(
					stage,
					progresses.groupBy(identity).mapValues(_.size).toSeq.map { case (p, c) => MarkingStageProgress(p, c) }
				))
			} else {
				Nil
			}
		}

		val allNextStages =
			workflowStudents.flatMap(_.nextStage).groupBy(identity).mapValues(_.size)

		val nextStages =
			allStages.filter(allNextStages.contains).map { stage =>
				MarkingNextStage(
					stage,
					allNextStages(stage)
				)
			}

		MarkerAssignmentInfo(
			assignment,
			assignment.feedbackDeadline,
			extensionCount = assignment.extensions.asScala.count { e => students.exists(e.isForUser) },
			unsubmittedCount = students.count { u => !assignment.submissions.asScala.exists(_.isForUser(u)) },
			lateSubmissionsCount = assignment.submissions.asScala.count { s => s.isLate && students.exists(s.isForUser) },
			currentStages,
			stages,
			nextStages
		)
	}
}