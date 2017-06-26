package uk.ac.warwick.tabula.commands.cm2

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.WorkflowStages.StageProgress
import uk.ac.warwick.tabula._
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
import uk.ac.warwick.tabula.services.cm2.{AutowiringCM2WorkflowProgressServiceComponent, CM2WorkflowProgressServiceComponent, CM2WorkflowStages}
import uk.ac.warwick.tabula.services.permissions.{AutowiringCacheStrategyComponent, CacheStrategyComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.cache._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

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

	def apply(user: CurrentUser): Command =
		new CourseworkHomepageCommandInternal(user)
			with ComposableCommand[Result]
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringAssessmentServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringCM2MarkingWorkflowServiceComponent
			with MarkerProgress
			with CommandWorkflowStudentsForAssignment
			with CachedMarkerWorkflowInformation
			with AutowiringCacheStrategyComponent
			with AutowiringCM2WorkflowProgressServiceComponent
			with PubliclyVisiblePermissions with Unaudited with ReadOnly
}

trait CourseworkHomepageCommandState {
	def user: CurrentUser
}

class CourseworkHomepageCommandInternal(val user: CurrentUser) extends CommandInternal[Result]
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
		assessmentService.getAssignmentsWithFeedback(user.userId, None).filter(_.publishFeedback) // Any academic year
	}

	private lazy val assignmentsWithSubmission = benchmarkTask("Get assignments with submission") {
		assessmentService.getAssignmentsWithSubmission(user.userId, None) // Any academic year
	}

	private lazy val enrolledAssignments = benchmarkTask("Get enrolled assignments") {
		assessmentMembershipService.getEnrolledAssignments(user.apparentUser, None) // Any academic year
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
		allUnsubmittedAssignments
			.diff(studentUpcomingAssignments)
			.filter(_.submittable)
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
		val submittedAwaitingFeedback =
			assignmentsWithSubmission
				.diff(assignmentsWithFeedback)
				.filter(_.publishFeedback)
				.map(enhance)

		val unsubmittedAndUnsubmittable =
			allUnsubmittedAssignments
				.diff(studentUpcomingAssignments)
				.diff(studentActionRequiredAssignments)

		(submittedAwaitingFeedback ++ unsubmittedAndUnsubmittable)
			.sortWith(hasEarlierEffectiveDate)
	}

	lazy val studentCompletedAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get past assignments") {
		(assignmentsWithFeedback ++ enrolledAssignments.filter(lateFormative) ++ assignmentsWithSubmission.filterNot(_.publishFeedback))
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
			info.assignment.feedbacks.asScala.exists(_.markingInProgress.exists(_.marker == user.apparentUser))
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
		assessmentService.getAssignmentWhereMarker(user.apparentUser, None) // Any academic year
			.map { assignment =>
				val markingWorkflow = assignment.markingWorkflow
				val students = markingWorkflow.getMarkersStudents(assignment, user.apparentUser).toSet

				enhance(assignment, students)
			}
	}

	private lazy val allCM2MarkerAssignments: Seq[MarkerAssignmentInfo] = benchmarkTask("Get CM2 assignments for marking") {
		assessmentService.getCM2AssignmentsWhereMarker(user.apparentUser, None) // Any academic year
			.map { assignment =>
				val markerFeedbacks = cm2MarkingWorkflowService.getAllFeedbackForMarker(assignment, user.apparentUser).values.flatten
				val students = markerFeedbacks.map(_.student).toSet

				enhance(assignment, students)
			}
	}

	private lazy val allMarkerAssignments: Seq[MarkerAssignmentInfo] = benchmarkTask("Get assignments for marking") {
		(allCM1MarkerAssignments ++ allCM2MarkerAssignments)
			.sortBy(info => (info.assignment.openEnded, Option(info.assignment.closeDate)))
	}

}

trait WorkflowStudentsForAssignment {
	def workflowStudentsFor(assignment: Assignment): Seq[AssignmentSubmissionStudentInfo]
}

trait CommandWorkflowStudentsForAssignment extends WorkflowStudentsForAssignment {
	def workflowStudentsFor(assignment: Assignment): Seq[AssignmentSubmissionStudentInfo] = SubmissionAndFeedbackCommand(assignment).apply().students
}

object MarkerWorkflowInformation {
	type Usercode = String
	type StageName = String

	case class WorkflowProgressInformation(
		stages: Map[StageName, WorkflowStages.StageProgress],
		nextStage: Option[WorkflowStage]
	)
}

trait MarkerWorkflowInformation {
	import MarkerWorkflowInformation._

	def markerWorkflowInformation(assignment: Assignment): Map[Usercode, WorkflowProgressInformation]
}

object MarkerWorkflowCache {
	type AssignmentId = String
	type Json = String

	final val CacheName: String = "MarkerWorkflowInformation"

	/**
		* 1 day in seconds - note we can cache this for a reasonably long time because none of the *marking* related events are time based,
		* but if we extend caching this information to other places (which is probably a good idea) they *will* be time based and we will
		* probably have to look at a custom expiry based on the assignment itself (so that we don't cache across deadlines)
		*/
	final val CacheExpiryTime: Long = 60 * 60 * 24
}

trait MarkerWorkflowCache {
	self: WorkflowStudentsForAssignment with CacheStrategyComponent with AssessmentServiceComponent =>

	import MarkerWorkflowCache._
	import MarkerWorkflowInformation._

	private val markerWorkflowCacheEntryFactory = new CacheEntryFactory[AssignmentId, Json] {

		override def create(id: AssignmentId): Json = {
			val assignment = assessmentService.getAssignmentById(id).getOrElse { throw new CacheEntryUpdateException(s"Could not find assignment $id") }

			Try {
				toJson(workflowStudentsFor(assignment).map { student =>
					student.user.getUserId -> WorkflowProgressInformation(student.stages, student.nextStage)
				}.toMap)
			} match {
				case Success(info) => info
				case Failure(e) => throw new CacheEntryUpdateException(e)
			}
		}

		override def create(ids: JList[AssignmentId]): JMap[AssignmentId, Json] =
			JMap(ids.asScala.map(id => (id, create(id))): _*)

		override def isSupportsMultiLookups: Boolean = true
		override def shouldBeCached(info: Json): Boolean = true
	}

	private def toJson(markerInformation: Map[Usercode, WorkflowProgressInformation]): Json =
		JsonHelper.toJson(markerInformation.mapValues { progressInfo =>
			Map(
				"stages" -> progressInfo.stages,
				"nextStage" -> progressInfo.nextStage.map(_.toString)
			)
		})

	lazy val markerWorkflowCache: Cache[AssignmentId, Json] =
		Caches.newCache(CacheName, markerWorkflowCacheEntryFactory, CacheExpiryTime, cacheStrategy)
}

trait CachedMarkerWorkflowInformation extends MarkerWorkflowInformation with MarkerWorkflowCache {
	self: WorkflowStudentsForAssignment with CacheStrategyComponent with AssessmentServiceComponent =>

	import MarkerWorkflowCache._
	import MarkerWorkflowInformation._

	private def fromJson(json: Json): Map[Usercode, WorkflowProgressInformation] =
		JsonHelper.toMap[Map[String, Any]](json).mapValues { progressInfo =>
			WorkflowProgressInformation(
				stages = progressInfo("stages").asInstanceOf[Map[StageName, Map[String, Any]]].map { case (stageName, progress) =>
					stageName -> StageProgress(
						stage = CM2WorkflowStages.of(stageName),
						started = progress("started").asInstanceOf[Boolean],
						messageCode = progress("messageCode").asInstanceOf[String],
						health = WorkflowStageHealth.fromCssClass(progress("health").asInstanceOf[Map[String, Any]]("cssClass").asInstanceOf[String]),
						completed = progress("completed").asInstanceOf[Boolean],
						preconditionsMet = progress("preconditionsMet").asInstanceOf[Boolean]
					)
				},
				nextStage = progressInfo.get("nextStage") match {
					case Some(null) => None
					case Some(nextStage: String) => Some(CM2WorkflowStages.of(nextStage))
					case _ => None
				}
			)
		}

	def markerWorkflowInformation(assignment: Assignment): Map[Usercode, WorkflowProgressInformation] =
		fromJson(markerWorkflowCache.get(assignment.id))
}

trait MarkerProgress extends TaskBenchmarking {
	self: MarkerWorkflowInformation
		with CM2WorkflowProgressServiceComponent =>

	protected def enhance(assignment: Assignment, students: Set[User]): MarkerAssignmentInfo = benchmarkTask(s"Get progress information for ${assignment.name}") {
		val workflowStudents = markerWorkflowInformation(assignment).filterKeys(usercode => students.exists(_.getUserId == usercode))

		val allStages = workflowProgressService.getStagesFor(assignment).filter(_.markingRelated)

		val currentStages = allStages.flatMap { stage =>
			val name = stage.toString

			val progresses = workflowStudents.values.flatMap(_.stages.get(name)).filter(_.preconditionsMet)
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

			val progresses = workflowStudents.values.flatMap(_.stages.get(name))
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
			workflowStudents.values.flatMap(_.nextStage).groupBy(identity).mapValues(_.size)

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