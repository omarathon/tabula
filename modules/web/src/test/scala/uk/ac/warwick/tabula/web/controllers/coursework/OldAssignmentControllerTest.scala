package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.commands.coursework.assignments._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.web.controllers.TestControllerOverrides
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands.coursework.{CurrentUserSubmissionAndFeedbackCommandState, StudentSubmissionAndFeedbackCommandInternal}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.StudentSubmissionAndFeedbackCommand._

class OldAssignmentControllerTest extends TestBase with Mockito {

	/** Reusable set of test objects. Put your code in a new Fixtures { } */
	trait Fixtures {
		val user: CurrentUser
		val controller = new OldAssignmentController with TestControllerOverrides
		val assignment = newDeepAssignment("CS101")
		val module = assignment.module
		val form = SubmitAssignmentCommand.self(module, assignment, currentUser)
		val errors = new BindException(form, "command")

		val feedbackService = smartMock[FeedbackService]
		val feedback = new AssignmentFeedback()
		feedbackService.getAssignmentFeedbackByUniId(assignment, "0123456") returns Some(feedback) thenThrows new Error("I TOLD YOU ABOUT STAIRS BRO")

		val submissionService = smartMock[SubmissionService]
		submissionService.getSubmissionByUniId(assignment, "0123456") returns None

		val profileService = smartMock[ProfileService]

		val infoCommand = new StudentSubmissionAndFeedbackCommandInternal(module, assignment)
			with CurrentUserSubmissionAndFeedbackCommandState with FeedbackServiceComponent with ProfileServiceComponent
			with SubmissionServiceComponent with Appliable[StudentSubmissionInformation] {

			def currentUser = user
			def feedbackService = Fixtures.this.feedbackService
			def submissionService = Fixtures.this.submissionService
			def profileService = Fixtures.this.profileService

			def apply() = applyInternal()
		}
	}

	@Test
	def feedbackAccess() {
		withUser("cusebr", "0123456") {
			new Fixtures {
				val user = currentUser
				profileService.getMemberByUser(user.apparentUser, disableFilter = false, eagerLoad = false) returns None
				val mav = controller.view(infoCommand, form, errors)
				withClue(mav) { mav.map should contain key "feedback" }
			}
		}
	}

}