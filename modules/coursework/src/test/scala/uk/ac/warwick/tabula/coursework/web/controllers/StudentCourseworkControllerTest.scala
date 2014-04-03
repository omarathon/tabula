package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.{Features, FeaturesComponent, Fixtures, TestBase, Mockito}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, AssignmentService, AssignmentMembershipServiceComponent, AssignmentServiceComponent}

class StudentCourseworkControllerTest extends TestBase with Mockito {

	val student = Fixtures.student()
	val scyd = student.defaultYearDetails.get

	private trait Fixture {
		val assignment = newDeepAssignment("LA101")
		assignment.closeDate = new DateTime()

		val submission = Fixtures.submission("0000001")
		submission.submittedDate = new DateTime()
	}

	trait CommandTestSupport extends AssignmentServiceComponent
			with AssignmentMembershipServiceComponent
			with FeaturesComponent {
		val assignmentService = smartMock[AssignmentService]
		val assignmentMembershipService = smartMock[AssignmentMembershipService]
		val features = smartMock[Features]

		assignmentService.getAssignmentsWithFeedback(scyd) returns Seq()
		//assignmentService.filterAssignmentsByCourseAndYear(allAssignments, scyd) returns (None)
		assignmentService.getAssignmentsWithSubmission(scyd) returns Seq()
		assignmentMembershipService.getEnrolledAssignments(scyd.studentCourseDetails.student.asSsoUser) returns Seq()
	}

	@Test
	def historicalAssignmentsNoDupes {
		new Fixture {
			val  assignmentInfo = Map("assignment" -> assignment,
				"submission" -> Option(submission))

			// A late formative assignment that has also been submitted will appear in both of the following
			val assignmentsWithSubmissionInfo = Seq(assignmentInfo)
			val lateFormativeAssignmentsInfo = Seq(assignmentInfo)

			val gadgetCommand = new StudentCourseworkGadgetCommandInternal(scyd) with CommandTestSupport
			val historicalAssignmentsInfo1 = gadgetCommand.getHistoricAssignmentsInfo(Nil, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
			historicalAssignmentsInfo1.size should be(1)

			val memberOrUser = MemberOrUser(Fixtures.user())
			val fullScreenCommand = new StudentCourseworkFullScreenCommandInternal(memberOrUser) with CommandTestSupport
			val historicalAssignmentsInfo2 = gadgetCommand.getHistoricAssignmentsInfo(Nil, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
			historicalAssignmentsInfo2.size should be(1)
		}
	}

}
