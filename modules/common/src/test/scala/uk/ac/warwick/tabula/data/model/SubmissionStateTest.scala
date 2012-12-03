package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.SubmissionService

class SubmissionStateTest extends TestBase with Mockito {

	val submissionService = mock[SubmissionService]

	// test all valid state transitions
	@Test def stateTransitionTest() {

		Received.canTransitionTo(ReleasedForMarking) should be (true)
		ReleasedForMarking.canTransitionTo(DownloadedByMarker) should be (true)
		DownloadedByMarker.canTransitionTo(MarkingCompleted) should be (true)

		Received.canTransitionTo(MarkingCompleted) should be (true)
		ReleasedForMarking.canTransitionTo(MarkingCompleted) should be (true)
	}

}
