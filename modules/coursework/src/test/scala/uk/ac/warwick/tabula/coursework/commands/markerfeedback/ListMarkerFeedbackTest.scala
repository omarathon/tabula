package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, AppContextTestBase}
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.commands.assignments.ListMarkerFeedbackCommand
import uk.ac.warwick.tabula.services.{UserLookupService, UserLookupComponent}


class ListMarkerFeedbackTest extends AppContextTestBase with MarkingWorkflowWorld with Mockito {

	trait CommandTestSupport extends UserLookupComponent {
		val userLookup = mock[UserLookupService]
	}

	@Transactional @Test
	def firstMarkerTest() {
		withUser("cuslaj") {
			val command =	new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser) with CommandTestSupport
			val markerFeedbackCollections = command.applyInternal()

			markerFeedbackCollections.completedFeedback.size should be (0)
			markerFeedbackCollections.inProgressFeedback.size should be (3)
		}
		withUser("cuscav") {
			val command =	new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser) with CommandTestSupport
			val markerFeedbackCollections = command.applyInternal()

			markerFeedbackCollections.completedFeedback.size should be (0)
			markerFeedbackCollections.inProgressFeedback.size should be (2)
		}
	}

	@Transactional @Test
	def secondMarkerTest() {
		assignment.feedbacks.foreach{feedback =>
			val fmFeedback = feedback.retrieveFirstMarkerFeedback
			fmFeedback.state = MarkingState.MarkingCompleted
			val smFeedback = feedback.retrieveSecondMarkerFeedback
			smFeedback.state = MarkingState.ReleasedForMarking
		}

		withUser("cuslat") {
			val command =	new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser) with CommandTestSupport
			val markerFeedbackCollections = command.applyInternal()

			markerFeedbackCollections.completedFeedback.size should be (0)
			markerFeedbackCollections.inProgressFeedback.size should be (3)
		}
		withUser("cuday") {
			val command =	new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser) with CommandTestSupport
			val markerFeedbackCollections = command.applyInternal()

			markerFeedbackCollections.completedFeedback.size should be (0)
			markerFeedbackCollections.inProgressFeedback.size should be (2)
		}
	}
}


