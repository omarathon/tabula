package uk.ac.warwick.tabula.events

import ch.qos.logback.classic.Logger
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.coursework.assignments.{BulkModerationApprovalCommandInternal, BulkModerationApprovalDescription, FinaliseFeedbackComponent}
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User

class AuditLoggingEventListenerTest extends TestBase with Mockito {

	val testLogger: Logger = TestLoggerFactory.getTestLogger("uk.ac.warwick.AUDIT")
	val listener = new AuditLoggingEventListener

	@Test
	def bulkModerationApproval(): Unit = {
		val assignment = Fixtures.assignment("Essay")
		assignment.id = "assignmentId"
		assignment.module = Fixtures.module("cs118")
		assignment.module.id = "moduleId"

		val marker: User = new User("cuscav")
		val submitter: CurrentUser = new CurrentUser(realUser = new User("curef"), apparentUser = new User("curef"))
		val gradeGenerator: GeneratesGradesFromMarks = smartMock[GeneratesGradesFromMarks]

		val command = new BulkModerationApprovalCommandInternal(assignment, marker, submitter, gradeGenerator) with BulkModerationApprovalDescription with StateServiceComponent with FeedbackServiceComponent with FinaliseFeedbackComponent {
			override val eventName: String = "BulkModerationApproval"

			override val stateService: StateService = smartMock[StateService]
			override val feedbackService: FeedbackService = smartMock[FeedbackService]
			override def finaliseFeedback(assignment: Assignment, markerFeedbacks: Seq[MarkerFeedback]): Unit = {}
		}

		val feedback1 = Fixtures.assignmentFeedback(universityId = "0000001", userId = "u0000001")
		val markerFeedback1 = Fixtures.markerFeedback(feedback1)

		val feedback2 = Fixtures.assignmentFeedback(universityId = "0000002", userId = "u0000002")
		val markerFeedback2 = Fixtures.markerFeedback(feedback2)

		command.markerFeedback.add(markerFeedback1)
		command.markerFeedback.add(markerFeedback2)

		val beforeEvent = Event.fromDescribable(command)
		val event = Event.resultFromDescribable(command, (), "eventId")
		listener.afterCommand(event, (), beforeEvent)

		TestLoggerFactory.retrieveEvents(testLogger).map(_.getFormattedMessage) should be (Seq("{event_type=BulkModerationApproval, tabula={assignment=assignmentId, numFeedbackUpdated=2, module=moduleId, students=[u0000001, u0000002]}}"))
	}

}
