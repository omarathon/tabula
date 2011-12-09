package uk.ac.warwick.courses.data.model
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._

class AssignmentTest extends TestBase {
	@Test def academicYear {
		
		withFakeTime(dateTime(2011,NOVEMBER)) {
			new Assignment().academicYear.startYear should be (2012)
		}
		
		withFakeTime(dateTime(2011,MAY)) { 
			new Assignment().academicYear.startYear should be (2011)
		}
	}
	
}