package uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase

class MeetingRecordTest extends TestBase with Mockito {
	
	@Test def nothingYet {
		val meeting = new MeetingRecord
		
		meeting should not be ('approved)
	}

}