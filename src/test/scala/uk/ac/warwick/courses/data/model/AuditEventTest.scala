package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.TestBase
import org.junit.Test

class AuditEventTest extends TestBase {

	@Test def emptyGetters {
		val event = new AuditEvent
		event.related = Seq(event)
		event.submissionIds.size should be (0)
		event.feedbackIds.size should be (0)
		
		val jsonVal = """{"submissions":["1234","3232","9898"]}"""
		
		event.parsedData = Some( json.readValue(jsonVal, classOf[Map[String,Any]]) )
		event.submissionIds.size should be (3)
	} 
	
}