package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.{AppContextTestBase, Mockito}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.StateService
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.commands.assignments.ReleaseForMarkingCommand

class ReleaseForMarkingTest extends AppContextTestBase with Mockito {

	@Autowired var stateService:StateService =_

	@Transactional @Test
	def isReleased {
		withUser("cuslaj") {

			val assignment = newDeepAssignment()
			assignment.closeDate = DateTime.parse("2012-08-15T12:00")
			session.save(assignment)

			generateSubmission(assignment, "0678022")
			generateSubmission(assignment, "1170836")
			generateSubmission(assignment, "9170726")
			val command = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser)
			command.stateService = stateService

			command.students = assignment.submissions.map(_.universityId)
			assignment.feedbacks = command.applyInternal()

			assignment.feedbacks.size should be (3)
			val firstMarkerFeedback = assignment.feedbacks.map(_.firstMarkerFeedback)
			firstMarkerFeedback.size should be (3)
			firstMarkerFeedback.filter(_.state == MarkingState.ReleasedForMarking).size should be (3)
		}
	}


	def generateSubmission(assignment:Assignment, uniId: String) {
		val submission = new Submission()
		submission.assignment = assignment
		submission.universityId = uniId
		assignment.submissions.add(submission)
	}

}
