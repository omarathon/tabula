package uk.ac.warwick.courses.web.controllers

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import uk.ac.warwick.courses._
import uk.ac.warwick.courses.commands.assignments._
import org.springframework.validation.BindException
import uk.ac.warwick.courses.data._
import uk.ac.warwick.courses.data.model._

class AssignmentControllerTest extends TestBase with Mockito {

	/** Reusable set of test objects. Put your code in a new Fixtures { } */
	trait Fixtures {
		val user: CurrentUser
		val controller = new AssignmentController with TestControllerOverrides
		val assignment = newDeepAssignment("CS101")
		val module = assignment.module
		val form = new SubmitAssignmentCommand(assignment, currentUser)
		form.module = module
		val errors = new BindException(form, "command")
		
		val feedbackDao = smartMock[FeedbackDao]
		val feedback = new Feedback()
		val m = new org.mockito.MockitoMocker
		feedbackDao.getFeedbackByUniId(assignment, "0123456") returns Some(feedback) thenThrows new Error("I TOLD YOU ABOUT STAIRS BRO")
		controller.feedbackDao = feedbackDao
	}
	
	@Test
	def feedbackAccess {
		withUser("cusebr", "0123456") {
			new Fixtures {
				val user = currentUser
				val mav = controller.view(currentUser, form, errors)
				withClue(mav) { mav.map should contain key ("feedback") }
			}
		}
	}
	
}