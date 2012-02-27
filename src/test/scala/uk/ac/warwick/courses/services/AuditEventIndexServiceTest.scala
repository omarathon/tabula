package uk.ac.warwick.courses.services

import uk.ac.warwick.courses.TestBase
import org.apache.lucene.util.LuceneTestCase
import org.junit.Test
import uk.ac.warwick.courses.Mockito
import org.junit.After
import org.junit.Before
import org.joda.time.DateTime
import uk.ac.warwick.courses.data.model.AuditEvent
import org.apache.lucene.index.IndexReader
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import uk.ac.warwick.userlookup.User

class AuditEventIndexServiceTest extends TestBase with Mockito {
	
	@Test def index {
		val TEMP_DIR = createTemporaryDirectory
		
		val jsonData = Map(
					"students" -> Array("0123456", "0199999")
				)
		val jsonDataString = json.writeValueAsString(jsonData)
		
		val defendEvents = for (i <- 1 to 1000)
			yield AuditEvent(
				id="d"+i, eventId="d"+i, eventType="DefendBase", eventStage="before", userId="jim",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(i)
			)
		
		val publishEvents = for (i <- 1 to 20)
			yield AuditEvent(
				id="s"+i, eventId="s"+i, eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(i),
				data=jsonDataString
			)
		
		val events = defendEvents ++ publishEvents
		
		val service = mock[AuditEventService]
		service.parseData(null) returns None
		service.parseData(jsonDataString) returns Some(jsonData)
		service.listNewerThan(new DateTime(1970,1,1,0,0), 1000) returns events 
		
		val indexer = new AuditEventIndexService
		indexer.service = service
		indexer.indexPath = TEMP_DIR
		indexer.afterPropertiesSet
		
		indexer.index
		
		there was atLeastOne(service).parseData(null)
		there was atLeastOne(service).parseData(jsonDataString)
		
		val user = new User("jeb")
		user.setWarwickId("0123456")
		
		indexer.student(user).size should be (20)
		
		
	}
}