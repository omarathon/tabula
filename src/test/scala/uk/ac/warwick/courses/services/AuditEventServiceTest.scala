package uk.ac.warwick.courses.services

import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.courses.AppContextTestBase
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.events.Event
import org.springframework.transaction.annotation.Transactional
import org.joda.time.DateTime

class AuditEventServiceTest extends AppContextTestBase {
	
	@Autowired var service:AuditEventService =_
	
	@Transactional
	@Test def listEvents {
		val now = new DateTime()
		for (i <- Range(1,30)) {
			val event = new Event("Bite"+i, "cusebr", "cusebr", Map(), now.plusSeconds(i))
			service.save(event, "pre")
		}
		
		val recent = service.listRecent(5,10)
		recent.size should be (10)
		recent.get(0).eventType should be ("Bite24")
		recent.get(2).eventType should be ("Bite22")
	}
}