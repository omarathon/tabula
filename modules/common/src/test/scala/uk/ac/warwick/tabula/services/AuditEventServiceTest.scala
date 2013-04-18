package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.events.Event

import org.joda.time.DateTime
import org.hibernate.dialect.HSQLDialect

class AuditEventServiceTest extends AppContextTestBase {
	
	lazy val service = Wire[AuditEventService]
	
	@Test def listEvents = transactional { tx =>
		
		val now = new DateTime()
		for (i <- Range(1,30)) {
			val event = new Event("1138-9962-1813-4938", "Bite"+i, "cusebr", "cusebr", Map(), now.plusSeconds(i))
			service.save(event, "pre")
		}
		
		val recent = service.listRecent(5,10).toList
		recent.size should be (10)
		recent(0).eventType should be ("Bite24")
		recent(2).eventType should be ("Bite22")
	}
}