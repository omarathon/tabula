package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.SubmissionState
import uk.ac.warwick.tabula.data.model.MarkingCompleted
import uk.ac.warwick.tabula.Mockito
import org.hibernate.classic.Session

class SubmissionServiceTest extends TestBase with Mockito {
	
	val mockSession = mock[Session]
	val service = new SubmissionServiceImpl {
		override def session = mockSession
	}
	
	@Test
	def nullState {
		val submission = new Submission
		service.updateState(submission, MarkingCompleted)
		submission.state should be (MarkingCompleted)
	}

}