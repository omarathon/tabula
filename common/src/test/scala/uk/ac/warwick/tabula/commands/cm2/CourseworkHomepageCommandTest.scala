package uk.ac.warwick.tabula.commands.cm2

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

// scalastyle:off public.methods.have.type
// scalastyle:off public.property.type.annotation

class CourseworkHomepageCommandTest extends TestBase with Mockito {

	private trait StudentCommandFixture {
		val command = new CourseworkHomepageStudentAssignments with CourseworkHomepageCommandState with AssessmentServiceComponent with AssessmentMembershipServiceComponent {
			val assessmentService = smartMock[AssessmentService]
			val assessmentMembershipService = smartMock[AssessmentMembershipService]

			val academicYear = AcademicYear(2016)
			val user = currentUser
		}
	}

	@Test
	def studentInfo(): Unit = withUser("cuscav") { new StudentCommandFixture {
		val extensionService = smartMock[ExtensionService]

		val enrolled1 = Fixtures.assignment("Enrolled assignment 1")
		enrolled1.extensionService = extensionService
		enrolled1.closeDate = new DateTime(2016, DateTimeConstants.JULY, 4, 14, 0, 0, 0)

		val enrolled2 = Fixtures.assignment("Enrolled assignment 2")
		enrolled2.extensionService = extensionService
		enrolled2.closeDate = new DateTime(2016, DateTimeConstants.AUGUST, 8, 14, 0, 0, 0)

		val feedback1 = Fixtures.assignment("Assignment with feedback 1")
		feedback1.extensionService = extensionService
		feedback1.closeDate = new DateTime(2016, DateTimeConstants.AUGUST, 4, 14, 0, 0, 0)

		val feedback2 = Fixtures.assignment("Assignment with feedback 2")
		feedback2.extensionService = extensionService
		feedback2.closeDate = new DateTime(2016, DateTimeConstants.JUNE, 4, 14, 0, 0, 0)

		val submitted1 = Fixtures.assignment("Submitted assignment 1")
		submitted1.extensionService = extensionService
		submitted1.closeDate = new DateTime(2016, DateTimeConstants.JUNE, 4, 11, 30, 0, 0)

		val submitted2 = Fixtures.assignment("Submitted assignment 2")
		submitted2.extensionService = extensionService
		submitted2.closeDate = new DateTime(2016, DateTimeConstants.MAY, 4, 14, 0, 0, 0)

		command.assessmentMembershipService.getEnrolledAssignments(command.user.apparentUser, Some(command.academicYear)) returns Seq(enrolled1, enrolled2)
		command.assessmentService.getAssignmentsWithFeedback("cuscav", Some(command.academicYear)) returns Seq(feedback1, feedback2)
		command.assessmentService.getAssignmentsWithSubmission("cuscav", Some(command.academicYear)) returns Seq(submitted1, submitted2)

		val info = command.studentInformation
		info.unsubmittedAssignments should have size 2
		info.inProgressAssignments should have size 2
		info.pastAssignments should have size 2
	}}

	@Test
	def enhanceAssignment(): Unit = withUser("cuscav", "0672089") { withFakeTime(new DateTime(2016, DateTimeConstants.JULY, 10, 14, 0, 0, 0)) { new StudentCommandFixture {
		val assignment = Fixtures.assignment("Essay")
		assignment.extensionService = smartMock[ExtensionService]
		assignment.openDate = new DateTime(2010, DateTimeConstants.JULY, 4, 14, 0, 0, 0)
		assignment.closeDate = new DateTime(2016, DateTimeConstants.JULY, 4, 14, 0, 0, 0)
		assignment.collectSubmissions = true
		assignment.allowLateSubmissions = true
		assignment.allowResubmission = true

		val submission = Fixtures.submission("0672089", "cuscav")
		assignment.submissions.add(submission)

		val extension = Fixtures.extension("0672089", "cuscav")
		extension.approve()
		extension.expiryDate = new DateTime(2016, DateTimeConstants.JULY, 25, 10, 0, 0, 0)
		assignment.extensions.add(extension)
		extension.assignment = assignment

		val feedback = Fixtures.assignmentFeedback("0672089", "cuscav")
		feedback.released = true
		assignment.feedbacks.add(feedback)

		val info = command.enhance(assignment)
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
			val moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]

			val academicYear = AcademicYear(2016)
			val user = currentUser
		}
	}

	@Test
	def adminInfo(): Unit = withUser("cuscav") { new AdminCommandFixture {
		val cs = Fixtures.department("cs", "Computer Science")
		val po = Fixtures.department("po", "Politics")
		val es = Fixtures.department("es", "Engineering")

		val cs118 = Fixtures.module("cs118")
		cs118.adminDepartment = cs

		val cs120 = Fixtures.module("cs120")
		cs120.adminDepartment = cs

		val po101 = Fixtures.module("po101")
		po101.adminDepartment = po

		command.moduleAndDepartmentService.modulesWithPermission(command.user, Permissions.Module.ManageAssignments) returns Set(cs120, po101, cs118)
		command.moduleAndDepartmentService.departmentsWithPermission(command.user, Permissions.Module.ManageAssignments) returns Set(po, es)

		command.moduleManagerDepartments should be (Seq(cs, po))
		command.adminDepartments should be (Seq(es, po))
	}}

}
