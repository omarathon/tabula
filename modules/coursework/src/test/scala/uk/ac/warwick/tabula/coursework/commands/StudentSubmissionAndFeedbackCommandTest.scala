package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import org.joda.time.DateTime
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}

class StudentSubmissionAndFeedbackCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends StudentSubmissionAndFeedbackCommandState 
		with SubmissionServiceComponent with FeedbackServiceComponent with ProfileServiceComponent {
		val submissionService = smartMock[SubmissionService]
		val feedbackService = smartMock[FeedbackService]
		val profileService = smartMock[ProfileService]
	}

	private trait Fixture {
		val module = Fixtures.module("in101")
		val assignment = Fixtures.assignment("Writing test")
		assignment.allowResubmission = false
		assignment.openEnded = true
		assignment.openDate = DateTime.now.minusDays(1)
		assignment.collectSubmissions = true

		val user = new User("cuscav")
		user.setWarwickId("0672089")
	}

	private trait CommandFixture extends Fixture {
		val command = new StudentSubmissionAndFeedbackCommandInternal(module, assignment) with CommandTestSupport {
			val studentUser = user
			val viewer = user
		}
	}

	@Test def apply() { new CommandFixture {
		val submission = Fixtures.submission()
		submission.submitted = true

		val feedback = Fixtures.assignmentFeedback()
		feedback.released = true

		val extension = Fixtures.extension("0672089", "cuscav")
		extension.approve()
		extension.expiryDate = DateTime.now.plusDays(5)

		assignment.extensions.add(extension)

		command.submissionService.getSubmissionByUniId(assignment, "0672089") returns Some(submission)
		command.feedbackService.getAssignmentFeedbackByUniId(assignment, "0672089") returns Some(feedback)

		val info = command.applyInternal()
		info.submission should be (Some(submission))
		info.feedback should be (Some(feedback))
		info.extension should be (Some(extension))
		info.isExtended should be {true}
		info.extensionRequested should be {false}
		info.canSubmit should be {true}
		info.canReSubmit should be {false}
	}}

	@Test def currentUserPermissions() { withUser("cuscav", "0672089") {
		val u = currentUser
		val command = new CurrentUserSubmissionAndFeedbackCommandPermissions with CurrentUserSubmissionAndFeedbackCommandState with CommandTestSupport {
			val module = Fixtures.module("in101")
			val assignment = Fixtures.assignment("Writing")
			assignment.module = module

			val currentUser = u
		}

		command.submissionService.getSubmissionByUniId(command.assignment, "0672089") returns None
		command.feedbackService.getAssignmentFeedbackByUniId(command.assignment, "0672089") returns None

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheckAny(Seq(CheckablePermission(Permissions.Submission.Create, Some(command.assignment))))
	}}

	@Test def memberPermissions() {
		val m = Fixtures.student("0672089", "cuscav")
		val command = new StudentMemberSubmissionAndFeedbackCommandPermissions with StudentMemberSubmissionAndFeedbackCommandState with CommandTestSupport {
			val module = Fixtures.module("in101")
			val assignment = Fixtures.assignment("Writing")
			assignment.module = module

			val studentMember = m
			val currentUser = smartMock[CurrentUser]
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.Submission.Read, m)
		verify(checking, times(1)).PermissionCheck(Permissions.AssignmentFeedback.Read, m)
	}

}
