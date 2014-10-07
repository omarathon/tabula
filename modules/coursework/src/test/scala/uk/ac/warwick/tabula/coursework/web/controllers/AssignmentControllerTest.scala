package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.coursework.commands.assignments._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.web.controllers.TestControllerOverrides
import uk.ac.warwick.tabula.services.{SubmissionService, SubmissionServiceComponent, FeedbackServiceComponent, FeedbackService}
import uk.ac.warwick.tabula.coursework.commands.{CurrentUserSubmissionAndFeedbackCommandState, StudentSubmissionAndFeedbackCommandInternal}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.commands.StudentSubmissionAndFeedbackCommand._

class AssignmentControllerTest extends TestBase with Mockito {

	/** Reusable set of test objects. Put your code in a new Fixtures { } */
	trait Fixtures {
		val user: CurrentUser
		val controller = new AssignmentController with TestControllerOverrides
		val assignment = newDeepAssignment("CS101")
		val module = assignment.module
		val form = new SubmitAssignmentCommand(module, assignment, currentUser)
		val errors = new BindException(form, "command")
		
		val feedbackService = smartMock[FeedbackService]
		val feedback = new Feedback()
		val m = new org.mockito.MockitoMocker
		feedbackService.getFeedbackByUniId(assignment, "0123456") returns Some(feedback) thenThrows new Error("I TOLD YOU ABOUT STAIRS BRO")

		val submissionService = smartMock[SubmissionService]
		submissionService.getSubmissionByUniId(assignment, "0123456") returns None

		val infoCommand = new StudentSubmissionAndFeedbackCommandInternal(module, assignment) with CurrentUserSubmissionAndFeedbackCommandState with FeedbackServiceComponent with SubmissionServiceComponent with Appliable[StudentSubmissionInformation] {
			def currentUser = user
			def feedbackService = Fixtures.this.feedbackService
			def submissionService = Fixtures.this.submissionService

			def apply() = applyInternal()
		}
	}

	@Test
	def feedbackAccess() {
		withUser("cusebr", "0123456") {
			new Fixtures {
				val user = currentUser
				val mav = controller.view(infoCommand, form, errors)
				withClue(mav) { mav.map should contain key "feedback" }
			}
		}
	}
	
}