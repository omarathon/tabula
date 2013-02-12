package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.StateService

class MarkingStateTest extends TestBase with Mockito {

	// test all valid state transitions
	@Test def stateTransitionTest() {

		ReleasedForMarking.canTransitionTo(DownloadedByMarker) should be (true)
		DownloadedByMarker.canTransitionTo(MarkingCompleted) should be (true)
		ReleasedForMarking.canTransitionTo(MarkingCompleted) should be (true)
	}

}
