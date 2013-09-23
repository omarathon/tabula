package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.MarkingState._

class MarkingStateUserTypeTest extends TestBase {
  
	@Test def convertToObject() {
		val t = new MarkingStateUserType
		t.convertToObject("Received") should be (Received)
		t.convertToObject("ReleasedForMarking") should be (ReleasedForMarking)
		t.convertToObject("DownloadedByMarker") should be (InProgress)
		t.convertToObject("MarkingCompleted") should be (MarkingCompleted)
		evaluating { t.convertToObject("Q") } should produce [IllegalArgumentException]
	}
  
	@Test def convertToValue() {
		val t = new MarkingStateUserType
		t.convertToValue(Received) should be ("Received")
		t.convertToValue(ReleasedForMarking) should be ("ReleasedForMarking")
		t.convertToValue(InProgress) should be ("DownloadedByMarker")
		t.convertToValue(MarkingCompleted) should be ("MarkingCompleted")
	}

}