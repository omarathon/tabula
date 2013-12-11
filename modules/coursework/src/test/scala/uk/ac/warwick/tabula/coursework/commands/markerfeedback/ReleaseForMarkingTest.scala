package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.{MockUserLookup, AppContextTestBase, Mockito}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.coursework.commands.assignments.{ReleaseForMarkingCommandTestSupport, ReleaseForMarkingCommand}
import uk.ac.warwick.tabula.services.{FeedbackService, StateService, AssignmentService, FeedbackServiceComponent, StateServiceComponent, AssignmentServiceComponent}
import uk.ac.warwick.tabula.data.model.MarkingState.ReleasedForMarking
import org.mockito.Mockito._
import uk.ac.warwick.tabula.Mockito

class ReleaseForMarkingTest extends AppContextTestBase with Mockito {

	trait TestSupport extends AssignmentServiceComponent with StateServiceComponent with FeedbackServiceComponent {

		val assignmentService = mock[AssignmentService]
		val stateService = mock[StateService]
		val feedbackService = mock[FeedbackService]
		def apply(): List[Feedback] = List()
	}

	@Transactional @Test
	def testIsReleased() {
		withUser("cuslaj") {

			val assignment = newDeepAssignment()
			val allStudentsUserGroup = UserGroup.ofUniversityIds
			allStudentsUserGroup.includeUsers = Seq("0678022","1170836","9170726")
			allStudentsUserGroup.userLookup = new MockUserLookup(true)
			val markerMap = Map("marker-uni-id"->allStudentsUserGroup)
			assignment.closeDate = DateTime.parse("2012-08-15T12:00")

			assignment.markerMap = markerMap
			session.save(assignment)

			generateSubmission(assignment, "0678022")
			generateSubmission(assignment, "1170836")
			generateSubmission(assignment, "9170726")

			// override studentsWithKnownMarkers so we dont have to mock-up a whole workflow
			val command = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with TestSupport {
				override def studentsWithKnownMarkers = Seq("0678022", "1170836", "9170726")
			}


			command.students = assignment.submissions.map(_.universityId)
			assignment.feedbacks = command.applyInternal()

			verify(command.stateService, times(3)).updateState(any[MarkerFeedback], any[MarkingState])

			assignment.feedbacks.size should be (3)
			val firstMarkerFeedback = assignment.feedbacks.map(_.firstMarkerFeedback)
			firstMarkerFeedback.size should be (3)
		}
	}


	def generateSubmission(assignment:Assignment, uniId: String) {
		val submission = new Submission()
		submission.assignment = assignment
		submission.universityId = uniId
		assignment.submissions.add(submission)
	}

}
