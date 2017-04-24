package uk.ac.warwick.tabula.commands.cm2

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.CourseworkHomepageCommand._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, Department, Submission}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

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
		unsubmittedAssignments: Seq[StudentAssignmentInformation],
		inProgressAssignments: Seq[StudentAssignmentInformation],
		pastAssignments: Seq[StudentAssignmentInformation]
	) {
		def isEmpty: Boolean = unsubmittedAssignments.isEmpty && inProgressAssignments.isEmpty && pastAssignments.isEmpty
		def nonempty: Boolean = !isEmpty
	}

	case class CourseworkHomepageInformation(
		studentInformation: CourseworkHomepageStudentInformation,
		homeDepartment: Option[Department],
		moduleManagerDepartments: Seq[Department],
		adminDepartments: Seq[Department]
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
			with PubliclyVisiblePermissions with Unaudited with ReadOnly
}

trait CourseworkHomepageCommandState {
	def academicYear: AcademicYear
	def user: CurrentUser
}

class CourseworkHomepageCommandInternal(val academicYear: AcademicYear, val user: CurrentUser) extends CommandInternal[Result]
	with CourseworkHomepageCommandState
	with CourseworkHomepageStudentAssignments
	with CourseworkHomepageAdminDepartments
	with TaskBenchmarking {
	self: ModuleAndDepartmentServiceComponent
		with AssessmentServiceComponent
		with AssessmentMembershipServiceComponent =>

	override def applyInternal(): Result =
		CourseworkHomepageInformation(
			studentInformation,
			homeDepartment,
			moduleManagerDepartments,
			adminDepartments
		)

}

trait CourseworkHomepageAdminDepartments extends TaskBenchmarking {
	self: CourseworkHomepageCommandState with ModuleAndDepartmentServiceComponent =>

	lazy val homeDepartment: Option[Department] = benchmarkTask("Get user's home department") {
		user.departmentCode.maybeText.flatMap(moduleAndDepartmentService.getDepartmentByCode)
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
			unsubmittedAssignments,
			inProgressAssignments,
			pastAssignments
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

	lazy val unsubmittedAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get un-submitted assignments") {
		enrolledAssignments
			.diff(assignmentsWithFeedback)
			.diff(assignmentsWithSubmission)
			.filter(_.collectSubmissions) // TAB-475
			.filterNot(lateFormative)
			.sortWith(hasEarlierPersonalDeadline)
			.map(enhance)
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

	lazy val inProgressAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get in-progress assignments") {
		assignmentsWithSubmission
			.diff(assignmentsWithFeedback)
			.map(enhance)
			.sortWith(hasEarlierEffectiveDate)
	}

	lazy val pastAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get past assignments") {
		(assignmentsWithFeedback ++ enrolledAssignments.filter(lateFormative))
			.map(enhance)
			.sortWith(hasEarlierEffectiveDate)
	}

}