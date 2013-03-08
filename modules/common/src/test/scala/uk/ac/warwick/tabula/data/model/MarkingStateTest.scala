package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.MarkingState._

class MarkingStateTest extends TestBase with Mockito {

	// test all valid state transitions
	@Test def stateTransitionTest() {
		Received.canTransitionTo(MarkingCompleted) should be (false)
		MarkingCompleted.canTransitionTo(DownloadedByMarker) should be (false)
		ReleasedForMarking.canTransitionTo(DownloadedByMarker) should be (true)
		DownloadedByMarker.canTransitionTo(MarkingCompleted) should be (true)
		ReleasedForMarking.canTransitionTo(MarkingCompleted) should be (true)
	}

}
