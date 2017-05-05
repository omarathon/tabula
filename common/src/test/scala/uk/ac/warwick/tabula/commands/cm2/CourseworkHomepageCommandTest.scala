package uk.ac.warwick.tabula.commands.cm2

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.{CM2WorkflowProgressService, CM2WorkflowProgressServiceComponent, CM2WorkflowStage, CM2WorkflowStages}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.cm2.CourseworkHomepageCommand.{CourseworkHomepageMarkerInformation, MarkerAssignmentInfo}
import uk.ac.warwick.tabula.data.model.markingworkflow.{CM2MarkingWorkflow, DoubleWorkflow}
import uk.ac.warwick.tabula.helpers.cm2.WorkflowStudent
import uk.ac.warwick.tabula.services.cm2.CM2WorkflowStages.{CM2MarkingWorkflowStage, CM2ReleaseForMarking, CheckForPlagiarism}

import scala.collection.immutable.SortedMap

// scalastyle:off public.methods.have.type
// scalastyle:off public.property.type.annotation

class CourseworkHomepageCommandTest extends TestBase with Mockito {

	private trait StudentCommandFixture {
		val command = new CourseworkHomepageStudentAssignments with CourseworkHomepageCommandState with AssessmentServiceComponent with AssessmentMembershipServiceComponent {
			val assessmentService: AssessmentService = smartMock[AssessmentService]
			val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]

			val academicYear = AcademicYear(2016)
			val user: CurrentUser = currentUser
		}
	}

	@Test
	def studentInfo(): Unit = withUser("cuscav") { new StudentCommandFixture {
		val extensionService: ExtensionService = smartMock[ExtensionService]

		val enrolled1: Assignment = Fixtures.assignment("Enrolled assignment 1")
		enrolled1.extensionService = extensionService
		enrolled1.closeDate = new DateTime(2016, DateTimeConstants.JULY, 4, 14, 0, 0, 0)

		val enrolled2: Assignment = Fixtures.assignment("Enrolled assignment 2")
		enrolled2.extensionService = extensionService
		enrolled2.closeDate = new DateTime(2016, DateTimeConstants.AUGUST, 8, 14, 0, 0, 0)

		val feedback1: Assignment = Fixtures.assignment("Assignment with feedback 1")
		feedback1.extensionService = extensionService
		feedback1.closeDate = new DateTime(2016, DateTimeConstants.AUGUST, 4, 14, 0, 0, 0)

		val feedback2: Assignment = Fixtures.assignment("Assignment with feedback 2")
		feedback2.extensionService = extensionService
		feedback2.closeDate = new DateTime(2016, DateTimeConstants.JUNE, 4, 14, 0, 0, 0)

		val submitted1: Assignment = Fixtures.assignment("Submitted assignment 1")
		submitted1.extensionService = extensionService
		submitted1.closeDate = new DateTime(2016, DateTimeConstants.JUNE, 4, 11, 30, 0, 0)

		val submitted2: Assignment = Fixtures.assignment("Submitted assignment 2")
		submitted2.extensionService = extensionService
		submitted2.closeDate = new DateTime(2016, DateTimeConstants.MAY, 4, 14, 0, 0, 0)

		command.assessmentMembershipService.getEnrolledAssignments(command.user.apparentUser, Some(command.academicYear)) returns Seq(enrolled1, enrolled2)
		command.assessmentService.getAssignmentsWithFeedback("cuscav", Some(command.academicYear)) returns Seq(feedback1, feedback2)
		command.assessmentService.getAssignmentsWithSubmission("cuscav", Some(command.academicYear)) returns Seq(submitted1, submitted2)

		val info: CourseworkHomepageCommand.CourseworkHomepageStudentInformation = command.studentInformation
		info.unsubmittedAssignments should have size 2
		info.inProgressAssignments should have size 2
		info.pastAssignments should have size 2
	}}

	@Test
	def enhanceAssignment(): Unit = withUser("cuscav", "0672089") { withFakeTime(new DateTime(2016, DateTimeConstants.JULY, 10, 14, 0, 0, 0)) { new StudentCommandFixture {
		val assignment: Assignment = Fixtures.assignment("Essay")
		assignment.extensionService = smartMock[ExtensionService]
		assignment.openDate = new DateTime(2010, DateTimeConstants.JULY, 4, 14, 0, 0, 0)
		assignment.closeDate = new DateTime(2016, DateTimeConstants.JULY, 4, 14, 0, 0, 0)
		assignment.collectSubmissions = true
		assignment.allowLateSubmissions = true
		assignment.allowResubmission = true

		val submission: Submission = Fixtures.submission("0672089", "cuscav")
		assignment.submissions.add(submission)

		val extension: Extension = Fixtures.extension("0672089", "cuscav")
		extension.approve()
		extension.expiryDate = new DateTime(2016, DateTimeConstants.JULY, 25, 10, 0, 0, 0)
		assignment.extensions.add(extension)
		extension.assignment = assignment

		val feedback: AssignmentFeedback = Fixtures.assignmentFeedback("0672089", "cuscav")
		feedback.released = true
		assignment.feedbacks.add(feedback)

		val info: CourseworkHomepageCommand.StudentAssignmentInformation = command.enhance(assignment)
		info.assignment should be (assignment)
		info.submission should be (Some(submission))
		info.extension should be (Some(extension))
		info.extended should be (true)
		info.hasActiveExtension should be (true)
		info.extensionRequested should be (false)
		info.studentDeadline should be (new DateTime(2016, DateTimeConstants.JULY, 25, 10, 0, 0, 0))
		info.submittable should be (true)
		info.resubmittable should be (true)
		info.feedback should be (Some(feedback))
		info.feedbackDeadline should be (Some(new LocalDate(2016, DateTimeConstants.AUGUST, 22)))
		info.feedbackLate should be (false)
	}}}

	private trait AdminCommandFixture {
		val command = new CourseworkHomepageAdminDepartments with CourseworkHomepageCommandState with ModuleAndDepartmentServiceComponent {
			val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]

			val academicYear = AcademicYear(2016)
			val user: CurrentUser = currentUser
		}
	}

	@Test
	def adminInfo(): Unit = withUser("cuscav") { new AdminCommandFixture {
		val cs: Department = Fixtures.department("cs", "Computer Science")
		val po: Department = Fixtures.department("po", "Politics")
		val es: Department = Fixtures.department("es", "Engineering")

		val cs118: Module = Fixtures.module("cs118")
		cs118.adminDepartment = cs

		val cs120: Module = Fixtures.module("cs120")
		cs120.adminDepartment = cs

		val po101: Module = Fixtures.module("po101")
		po101.adminDepartment = po

		command.moduleAndDepartmentService.modulesWithPermission(command.user, Permissions.Module.ManageAssignments) returns Set(cs120, po101, cs118)
		command.moduleAndDepartmentService.departmentsWithPermission(command.user, Permissions.Module.ManageAssignments) returns Set(po, es)

		command.moduleManagerDepartments should be (Seq(cs, po))
		command.adminDepartments should be (Seq(es, po))
	}}

	private trait MarkerCommandFixture {
		val command = new CourseworkHomepageMarkerAssignments with CourseworkHomepageCommandState with AssessmentServiceComponent
			with CM2MarkingWorkflowServiceComponent with CM2WorkflowProgressServiceComponent with MarkerProgress with WorkflowStudentsForAssignment {

			val assessmentService: AssessmentService = smartMock[AssessmentService]
			val cm2MarkingWorkflowService: CM2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]
			val workflowProgressService: CM2WorkflowProgressService = smartMock[CM2WorkflowProgressService]

			val academicYear = AcademicYear(2016)
			val user: CurrentUser = currentUser

			override def workflowStudentsFor(assignment: Assignment): Seq[WorkflowStudent] = Nil
		}
	}

	@Test
	def markerInfo(): Unit = withUser("cuscav") { new MarkerCommandFixture {
		val extensionService = smartMock[ExtensionService]

		val cm1MarkingWorkflow = Fixtures.seenSecondMarkingWorkflow("Seen second marking")
		val cm2MarkingWorkflow = new DoubleWorkflow

		def cm1Assignment(name: String) = {
			val assignment = Fixtures.assignment(name)
			assignment.markingWorkflow = cm1MarkingWorkflow
			assignment.extensionService = extensionService
			assignment.closeDate = DateTime.now.plusDays(1)
			assignment
		}

		def cm2Assignment(name: String) = {
			val assignment = Fixtures.assignment(name)
			assignment.cm2MarkingWorkflow = cm2MarkingWorkflow
			assignment.extensionService = extensionService
			assignment.closeDate = DateTime.now.plusDays(2)
			assignment
		}

		val a1 = cm1Assignment("Assignment 1")

		val a2 = cm2Assignment("Assignment 2")

		val a3 = cm1Assignment("Assignment 3")

		val a4 = cm2Assignment("Assignment 4")

		val a5 = cm1Assignment("Assignment 5")

		val a6 = cm2Assignment("Assignment 6")

		val a7 = cm1Assignment("Assignment 7")

		val a8 = cm2Assignment("Assignment 8")

		// CM1 assignments
		command.assessmentService.getAssignmentWhereMarker(currentUser.apparentUser, Some(command.academicYear)) returns Seq(a1, a3, a5, a7)

		// CM2 assignments
		command.assessmentService.getCM2AssignmentsWhereMarker(currentUser.apparentUser, Some(command.academicYear)) returns Seq(a2, a4, a6, a8)
		command.cm2MarkingWorkflowService.getAllFeedbackForMarker(a2, currentUser.apparentUser) returns SortedMap.empty
		command.cm2MarkingWorkflowService.getAllFeedbackForMarker(a4, currentUser.apparentUser) returns SortedMap.empty
		command.cm2MarkingWorkflowService.getAllFeedbackForMarker(a6, currentUser.apparentUser) returns SortedMap.empty
		command.cm2MarkingWorkflowService.getAllFeedbackForMarker(a8, currentUser.apparentUser) returns SortedMap.empty

		val cm1WorkflowStages: Seq[CM2WorkflowStage] = Seq(CM2WorkflowStages.Submission, CM2WorkflowStages.CheckForPlagiarism, CM2WorkflowStages.CM1ReleaseForMarking, CM2WorkflowStages.CM1FirstMarking, CM2WorkflowStages.CM1SecondMarking, CM2WorkflowStages.CM1FinaliseSeenSecondMarking)
		val cm2WorkflowStages: Seq[CM2WorkflowStage] = Seq(CM2WorkflowStages.Submission, CM2WorkflowStages.CheckForPlagiarism, CM2WorkflowStages.CM2ReleaseForMarking) ++ cm2MarkingWorkflow.allStages.map(CM2WorkflowStages.CM2MarkingWorkflowStage.apply)
		command.workflowProgressService.getStagesFor(a1) returns cm1WorkflowStages
		command.workflowProgressService.getStagesFor(a2) returns cm2WorkflowStages
		command.workflowProgressService.getStagesFor(a3) returns cm1WorkflowStages
		command.workflowProgressService.getStagesFor(a4) returns cm2WorkflowStages
		command.workflowProgressService.getStagesFor(a5) returns cm1WorkflowStages
		command.workflowProgressService.getStagesFor(a6) returns cm2WorkflowStages
		command.workflowProgressService.getStagesFor(a7) returns cm1WorkflowStages
		command.workflowProgressService.getStagesFor(a8) returns cm2WorkflowStages

		val a1Info = MarkerAssignmentInfo(a1, None, 0, 0, 0, Nil, Nil, Nil)
		val a2Info = MarkerAssignmentInfo(a2, None, 0, 0, 0, Nil, Nil, Nil)
		val a3Info = MarkerAssignmentInfo(a3, None, 0, 0, 0, Nil, Nil, Nil)
		val a4Info = MarkerAssignmentInfo(a4, None, 0, 0, 0, Nil, Nil, Nil)
		val a5Info = MarkerAssignmentInfo(a5, None, 0, 0, 0, Nil, Nil, Nil)
		val a6Info = MarkerAssignmentInfo(a6, None, 0, 0, 0, Nil, Nil, Nil)
		val a7Info = MarkerAssignmentInfo(a7, None, 0, 0, 0, Nil, Nil, Nil)
		val a8Info = MarkerAssignmentInfo(a8, None, 0, 0, 0, Nil, Nil, Nil)

		val info: CourseworkHomepageMarkerInformation = command.markerInformation
		// TODO we could test this better but there's a massive amount of boilerplate so it'd take ages.
		// Sorry to whoever finds this and wants to reproduce a bug
	}}

}
