package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.{Features, FeaturesComponent, Fixtures, TestBase, Mockito}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, AssignmentService, AssignmentMembershipServiceComponent, AssignmentServiceComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{StudentCourseYearDetails, Assignment}

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
		override val assignmentService = smartMock[AssignmentService]
		override val assignmentMembershipService = smartMock[AssignmentMembershipService]
		override val features = {
			val f = Features.empty
			f.assignmentMembership = true
			f
		}

		assignmentService.getAssignmentsWithFeedback(any[StudentCourseYearDetails]) returns Seq()
		assignmentService.filterAssignmentsByCourseAndYear(any[Seq[Assignment]], any[StudentCourseYearDetails]) returns Seq()
		assignmentService.getAssignmentsWithSubmission(any[StudentCourseYearDetails]) returns Seq()
		assignmentMembershipService.getEnrolledAssignments(any[User]) returns Seq()
		assignmentService.getAssignmentsWithFeedback(any[String]) returns Seq()
		assignmentService.getAssignmentsWithSubmission(any[String]) returns Seq()
	}

	@Test
	def historicalAssignmentsNoDupes {
		new Fixture {
			val  assignmentInfo = Map("assignment" -> assignment,
				"submission" -> Option(submission))

			// A late formative assignment that has also been submitted will appear in both of the following
			val assignmentsWithSubmissionInfo = Seq(assignmentInfo)
			val lateFormativeAssignmentsInfo = Seq(assignmentInfo)

			val gadgetCommand = new StudentCourseworkGadgetCommandInternal(scyd) with CommandTestSupport with StudentCourseworkCommandHelper
			val historicalAssignmentsInfo1 = gadgetCommand.getHistoricAssignmentsInfo(Nil, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
			historicalAssignmentsInfo1.size should be(1)

			val memberOrUser = MemberOrUser(Fixtures.user())
			val fullScreenCommand = new StudentCourseworkFullScreenCommandInternal(memberOrUser) with CommandTestSupport with StudentCourseworkCommandHelper
			val historicalAssignmentsInfo2 = gadgetCommand.getHistoricAssignmentsInfo(Nil, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
			historicalAssignmentsInfo2.size should be(1)
		}
	}

}
