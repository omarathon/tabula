package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.WorkflowStages.StageProgress
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.AssignmentInfoFilters.DueDateFilter
import uk.ac.warwick.tabula.commands.cm2.assignments.ListAssignmentsCommand._
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.markingworkflow.{FinalStage, MarkingWorkflowStage, MarkingWorkflowType}
import uk.ac.warwick.tabula.data.model.{Assignment, Department, MarkingMethod, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.{AutowiringCM2WorkflowProgressServiceComponent, CM2WorkflowCategory, CM2WorkflowProgressServiceComponent, CM2WorkflowStage}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, WorkflowStage}

import scala.collection.JavaConverters._

object ListAssignmentsCommand {
	trait AssignmentInfo {
		def assignment: Assignment
	}

	case class BasicAssignmentInfo(
		assignment: Assignment
	) extends AssignmentInfo

	case class AssignmentStageCategory(
		category: CM2WorkflowCategory,
		stages: Seq[AssignmentStage]
	)

	case class AssignmentStage(
		stage: CM2WorkflowStage,
		progress: Seq[AssignmentStageProgress]
	)

	case class AssignmentStageProgress(
		progress: StageProgress,
		count: Int
	)

	case class AssignmentNextStage(
		stage: WorkflowStage,
		title: Option[String],
		url: Option[String],
		count: Int
	)

	case class EnhancedAssignmentInfo(
		assignment: Assignment,
		stages: Seq[AssignmentStageCategory],
		nextStages: Seq[AssignmentNextStage]
	) extends AssignmentInfo

	case class ModuleAssignmentsInfo(
		module: Module,
		assignments: Seq[AssignmentInfo]
	)

	type DepartmentResult = Seq[ModuleAssignmentsInfo]
	type DepartmentCommand = Appliable[DepartmentResult] with ListDepartmentAssignmentsCommandState with ListAssignmentsModulesWithPermission

	type ModuleResult = ModuleAssignmentsInfo
	type ModuleCommand = Appliable[ModuleResult] with ListModuleAssignmentsCommandState

	val AdminPermission = Permissions.Module.ManageAssignments

	def department(department: Department, academicYear: AcademicYear, user: CurrentUser): DepartmentCommand =
		new ListDepartmentAssignmentsCommandInternal(department, academicYear, user)
			with ListDepartmentAssignmentsPermissions
			with ListAssignmentsModulesWithPermission
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringSecurityServiceComponent
			with ComposableCommand[DepartmentResult]
			with Unaudited with ReadOnly

	def module(module: Module, academicYear: AcademicYear, user: CurrentUser): ModuleCommand =
		new ListModuleAssignmentsCommandInternal(module, academicYear, user)
			with ListModuleAssignmentsPermissions
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringSecurityServiceComponent
			with AssignmentProgress
			with AutowiringCM2WorkflowProgressServiceComponent
			with ComposableCommand[ModuleResult]
			with Unaudited with ReadOnly

}

trait ListAssignmentsCommandState {
	def academicYear: AcademicYear
	def user: CurrentUser
}

trait ListDepartmentAssignmentsCommandState extends ListAssignmentsCommandState {
	def department: Department
}

trait ListModuleAssignmentsCommandState extends ListAssignmentsCommandState {
	def module: Module
}

trait ListAssignmentsCommandRequest {
	self: ListAssignmentsCommandState =>

	var moduleFilters: JList[AssignmentInfoFilters.Module] = JArrayList()
	var workflowTypeFilters: JList[AssignmentInfoFilter] = JArrayList()
	var statusFilters: JList[AssignmentInfoFilter] = JArrayList()
	var dueDateFilter: DueDateFilter = new DueDateFilter
	var showEmptyModules: Boolean = true
}

abstract class ListAssignmentsCommandInternal(val academicYear: AcademicYear, val user: CurrentUser)
	extends ListAssignmentsCommandState with ListAssignmentsCommandRequest {

	protected def moduleInfo(module: Module) = ModuleAssignmentsInfo(
		module,
		module.assignments.asScala.filter(_.isAlive).filter(_.academicYear == academicYear).map { assignment =>
			BasicAssignmentInfo(assignment)
		}.filter { info =>
			(moduleFilters.asScala.isEmpty || moduleFilters.asScala.exists(_(info))) &&
			(workflowTypeFilters.asScala.isEmpty || workflowTypeFilters.asScala.exists(_(info))) &&
			(statusFilters.asScala.isEmpty || statusFilters.asScala.exists(_(info))) &&
			dueDateFilter(info)
		}
	)

}

class ListDepartmentAssignmentsCommandInternal(val department: Department, academicYear: AcademicYear, user: CurrentUser)
	extends ListAssignmentsCommandInternal(academicYear, user)
		with CommandInternal[DepartmentResult]
		with ListDepartmentAssignmentsCommandState {
	self: ListAssignmentsModulesWithPermission =>

	override def applyInternal(): DepartmentResult =
		allModulesWithPermission.filter { module =>
			moduleFilters.asScala.isEmpty || moduleFilters.asScala.exists(_.module == module)
		}.map(moduleInfo).filter { info => showEmptyModules || info.assignments.nonEmpty }.sortBy(_.module.code)

}

class ListModuleAssignmentsCommandInternal(val module: Module, academicYear: AcademicYear, user: CurrentUser)
	extends ListAssignmentsCommandInternal(academicYear, user)
		with CommandInternal[ModuleResult]
		with ListModuleAssignmentsCommandState {
	self: AssignmentProgress
		with CM2WorkflowProgressServiceComponent =>

	override def applyInternal(): ModuleResult = {
		val info = moduleInfo(module)

		info.copy(assignments = info.assignments.map { a => enhance(a.assignment) })
	}

}

trait AssignmentProgress extends TaskBenchmarking {
	self: CM2WorkflowProgressServiceComponent =>

	def enhance(assignment: Assignment): EnhancedAssignmentInfo = benchmarkTask(s"Get progress information for ${assignment.name}") {
		val results = SubmissionAndFeedbackCommand(assignment).apply()

		val allStages = workflowProgressService.getStagesFor(assignment)

		val stages = allStages.flatMap { stage =>
			val name = stage.toString

			val progresses = results.students.flatMap(_.stages.get(name)).filter { p => p.started || p.completed }
			if (progresses.nonEmpty) {
				Seq(AssignmentStage(
					stage,
					progresses.groupBy(identity).mapValues(_.size).toSeq.map { case (p, c) => AssignmentStageProgress(p, c) }
				))
			} else {
				Nil
			}
		}.groupBy(_.stage.category)

		val stagesByCategory = CM2WorkflowCategory.members.map { category =>
			AssignmentStageCategory(category, stages.getOrElse(category, Nil))
		}.filterNot(_.stages.isEmpty)

		val allNextStages =
			results.students.flatMap(_.nextStage).groupBy(identity).mapValues(_.size)

		val nextStages =
			allStages.filter(allNextStages.contains).map { stage =>
				AssignmentNextStage(
					stage,
					stage.route(assignment).map(_.title),
					stage.route(assignment).map(_.url),
					allNextStages(stage)
				)
			}

		EnhancedAssignmentInfo(assignment, stagesByCategory, nextStages)
	}

}

trait ListAssignmentsModulesWithPermission {
	self: ListDepartmentAssignmentsCommandState
		with ModuleAndDepartmentServiceComponent
		with SecurityServiceComponent =>

	lazy val modulesWithPermission: Set[Module] =
		moduleAndDepartmentService.modulesWithPermission(user, AdminPermission, department)

	lazy val allModulesWithPermission: Seq[Module] =
		if (securityService.can(user, AdminPermission, department)) {
			department.modules.asScala
		} else {
			modulesWithPermission.toList.sorted
		}

}

trait ListDepartmentAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListDepartmentAssignmentsCommandState
		with ListAssignmentsModulesWithPermission
		with SecurityServiceComponent =>

	override def permissionsCheck(p: PermissionsChecking): Unit =
		if (securityService.can(user, AdminPermission, mandatory(department))) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			p.PermissionCheck(AdminPermission, department)
		} else {
			val managedModules = modulesWithPermission.toList

			// This is implied by the above, but it's nice to check anyway. Avoid exception if there are no managed modules
			if (managedModules.nonEmpty) p.PermissionCheckAll(AdminPermission, managedModules)
			else p.PermissionCheck(AdminPermission, department)
		}
}

trait ListModuleAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListModuleAssignmentsCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit =
		p.PermissionCheck(AdminPermission, mandatory(module))

}

sealed trait AssignmentInfoFilter {
	def description: String
	val getName: String = AssignmentInfoFilters.shortName(getClass)
	def apply(info: AssignmentInfo): Boolean
}

object AssignmentInfoFilters {
	private val ObjectClassPrefix = AssignmentInfoFilters.getClass.getName

	def shortName(clazz: Class[_ <: AssignmentInfoFilter]): String
		= clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')

	def of(name: String): AssignmentInfoFilter = {
		try {
			// Go through the magical hierarchy
			val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
			clz.getDeclaredField("MODULE$").get(null).asInstanceOf[AssignmentInfoFilter]
		} catch {
			case _: ClassNotFoundException => throw new IllegalArgumentException("AssignmentInfoFilter " + name + " not recognised")
			case _: ClassCastException => throw new IllegalArgumentException("AssignmentInfoFilter " + name + " is not an endpoint of the hierarchy")
		}
	}

	case class Module(module: model.Module) extends AssignmentInfoFilter {
		val description: String = module.code.toUpperCase() + " " + module.name
		override val getName: String = s"Module(${module.code})"
		def apply(info: AssignmentInfo): Boolean = info.assignment.module == module
	}

	def allModuleFilters(modules: Seq[model.Module]): Seq[Module] = modules.map(Module.apply)

	case class WorkflowType(method: MarkingMethod) extends AssignmentInfoFilter {
		val description: String = s"${method.description}-CM1"
		override val getName: String = method.name
		def apply(info: AssignmentInfo): Boolean = Option(info.assignment.markingWorkflow).map(_.markingMethod).contains(method)
	}

	case class CM2WorkflowType(method: MarkingWorkflowType) extends AssignmentInfoFilter {
		val description: String = method.description
		override val getName: String = method.name
		def apply(info: AssignmentInfo): Boolean = Option(info.assignment.cm2MarkingWorkflow).map(_.workflowType).contains(method)
	}

	def allWorkflowTypeFilters: Seq[AssignmentInfoFilter] = MarkingMethod.values.toSeq.map(WorkflowType.apply) ++ MarkingWorkflowType.values.map(CM2WorkflowType.apply)

	object Status {
		case object Active extends AssignmentInfoFilter {
			val description = "Active"
			def apply(info: AssignmentInfo): Boolean =
				info.assignment.isBetweenDates() || info.assignment.feedbackDeadline.exists(d => !d.isBefore(LocalDate.now))
		}
		case object Inactive extends AssignmentInfoFilter {
			val description = "Inactive"
			def apply(info: AssignmentInfo): Boolean =
				!info.assignment.isBetweenDates() && !info.assignment.feedbackDeadline.exists(d => !d.isBefore(LocalDate.now))
		}
		case object NoMarkers extends AssignmentInfoFilter {
			val description = "No markers"
			def apply(info: AssignmentInfo): Boolean = info.assignment.allFeedback
				.flatMap(_.allMarkerFeedback)
				.flatMap(m => Option(m.marker)) // markers may have been removed so could be null
				.isEmpty
		}
		case object ReleasedToStudents extends AssignmentInfoFilter {
			val description = "Released to students"
			def apply(info: AssignmentInfo): Boolean = info.assignment.hasReleasedFeedback
		}
		case object Closed extends AssignmentInfoFilter {
			val description = "Closed"
			def apply(info: AssignmentInfo): Boolean = info.assignment.isClosed
		}
		case object LateSubmissions extends AssignmentInfoFilter {
			val description = "Late submissions"
			def apply(info: AssignmentInfo): Boolean = info.assignment.lateSubmissionCount > 0
		}
		case object ExtensionsRequested extends AssignmentInfoFilter {
			val description = "Extensions requested"
			def apply(info: AssignmentInfo): Boolean = info.assignment.hasUnapprovedExtensions
		}
		case object NotCheckedForPlagiarism extends AssignmentInfoFilter {
			val description = "Not checked for plagiarism"
			def apply(info: AssignmentInfo): Boolean = !info.assignment.submissions.asScala.exists(_.hasOriginalityReport)
		}
		case object NotReleasedToMarkers extends AssignmentInfoFilter {
			val description = "Not released to markers"
			def apply(info: AssignmentInfo): Boolean = info.assignment.allFeedback.flatMap(_.outstandingStages.asScala).isEmpty
		}
		case object BeingMarked extends AssignmentInfoFilter {
			val description = "Being marked"
			def apply(info: AssignmentInfo): Boolean = info.assignment.allFeedback
				.flatMap(_.outstandingStages.asScala).flatMap{
					case f: FinalStage => None
					case s: MarkingWorkflowStage => Some(s)
				}.nonEmpty
		}
		case object MarkingComplete extends AssignmentInfoFilter {
			val description = "Marking complete"
			def apply(info: AssignmentInfo): Boolean = info.assignment.allFeedback
				.map(_.outstandingStages.asScala)
				.forall(stages => stages.nonEmpty && stages.collect{case f: FinalStage => f}.size == stages.size)
		}
		case object LateFeedback extends AssignmentInfoFilter {
			val description = "Late feedback"
			def apply(info: AssignmentInfo): Boolean = info.assignment.feedbackDeadline.exists(_.isBefore(LocalDate.now)) && info.assignment.hasOutstandingFeedback
		}
		case object FeedbackUnreleased extends AssignmentInfoFilter {
			val description = "Feedback unreleased"
			def apply(info: AssignmentInfo): Boolean = info.assignment.hasUnreleasedFeedback
		}

		val all = Seq(ReleasedToStudents, Closed, Active, Inactive, NoMarkers, LateSubmissions, ExtensionsRequested, NotCheckedForPlagiarism, NotReleasedToMarkers, BeingMarked, MarkingComplete, LateFeedback, FeedbackUnreleased)
	}

	class DueDateFilter extends AssignmentInfoFilter {
		var from: LocalDate = _
		var to: LocalDate = _

		override def description: String = "Between two dates"
		override def apply(info: AssignmentInfo): Boolean =
			(Option(from).isEmpty || !info.assignment.closeDate.isBefore(from.toDateTimeAtStartOfDay))&&
			(Option(to).isEmpty || !info.assignment.closeDate.isAfter(to.plusDays(1).toDateTimeAtStartOfDay))
	}
}