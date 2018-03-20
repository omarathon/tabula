package uk.ac.warwick.tabula.commands.cm2

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.{CM2WorkflowProgressService, CM2WorkflowProgressServiceComponent}

// scalastyle:off public.methods.have.type
// scalastyle:off public.property.type.annotation

class CourseworkHomepageCommandTest extends TestBase with Mockito {

	private trait StudentCommandFixture {
		val command = new CourseworkHomepageStudentAssignments with CourseworkHomepageCommandState with AssessmentServiceComponent with AssessmentMembershipServiceComponent {
			val assessmentService: AssessmentService = smartMock[AssessmentService]
			val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]

			val user: CurrentUser = currentUser
		}
	}

	@Test
	def studentInfo(): Unit = withUser("cuscav") { new StudentCommandFixture {
		val extensionService: ExtensionService = smartMock[ExtensionService]

		val enrolled1: Assignment = Fixtures.assignment("Enrolled assignment 1")
		enrolled1.extensionService = extensionService
		enrolled1.openDate = DateTime.now.minusDays(1)
		enrolled1.closeDate = new DateTime(2016, DateTimeConstants.JULY, 4, 14, 0, 0, 0)

		val enrolled2: Assignment = Fixtures.assignment("Enrolled assignment 2")
		enrolled2.extensionService = extensionService
		enrolled2.openDate = DateTime.now.minusDays(1)
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

		command.assessmentMembershipService.getEnrolledAssignments(command.user.apparentUser, None) returns Seq(enrolled1, enrolled2)
		command.assessmentService.getAssignmentsWithFeedback("cuscav", None) returns Seq(feedback1, feedback2)
		command.assessmentService.getAssignmentsWithSubmission("cuscav", None) returns Seq(submitted1, submitted2)

		val info: CourseworkHomepageCommand.CourseworkHomepageStudentInformation = command.studentInformation
		info.actionRequiredAssignments should have size 2
		info.noActionRequiredAssignments should have size 2
		info.completedAssignments should have size 2
		info.upcomingAssignments should have size 0
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
		assignment.addExtension(extension)

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
}
