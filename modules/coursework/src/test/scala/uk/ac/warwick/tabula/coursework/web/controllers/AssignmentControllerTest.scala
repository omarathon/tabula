package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.coursework.commands.assignments._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.web.controllers.TestControllerOverrides

import org.junit.Ignore

class AssignmentControllerTest extends TestBase with Mockito {

	/** Reusable set of test objects. Put your code in a new Fixtures { } */
	trait Fixtures {
		val user: CurrentUser
		val controller = new AssignmentController with TestControllerOverrides
		val assignment = newDeepAssignment("CS101")
		val module = assignment.module
		val form = new SubmitAssignmentCommand(module, assignment, currentUser)
		val errors = new BindException(form, "command")
		
		val feedbackDao = smartMock[FeedbackDao]
		val feedback = new Feedback()
		val m = new org.mockito.MockitoMocker
		feedbackDao.getFeedbackByUniId(assignment, "0123456") returns Some(feedback) thenThrows new Error("I TOLD YOU ABOUT STAIRS BRO")
		controller.feedbackDao = feedbackDao
	}
	
	@Ignore
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