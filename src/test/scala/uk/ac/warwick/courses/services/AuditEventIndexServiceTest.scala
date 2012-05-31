package uk.ac.warwick.courses.services

import uk.ac.warwick.courses.TestBase
import org.apache.lucene.util.LuceneTestCase
import org.junit.Test
import uk.ac.warwick.courses.Mockito
import uk.ac.warwick.courses.JavaImports._
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
import collection.JavaConversions._
import uk.ac.warwick.util.core.StopWatch
import uk.ac.warwick.courses.JsonObjectMapperFactory
import uk.ac.warwick.courses.helpers.ArrayList
import java.io.File

class AuditEventIndexServiceTest extends TestBase with Mockito {
	
	var indexer:AuditEventIndexService = _
	var service:AuditEventService = _
	var TEMP_DIR:File = _
	
	@Before def setup {		
		TEMP_DIR = createTemporaryDirectory
		
		service = mock[AuditEventService]
		service.parseData(any[String]) answers { _ match {
			case s:String => Some(readJsonMap(s))
			case _ => None
		}}
		
		val maintenanceMode = mock[MaintenanceModeService]
		
		indexer = new AuditEventIndexService
		indexer.service = service
		indexer.indexPath = TEMP_DIR
		indexer.maintenanceService = maintenanceMode
		indexer.afterPropertiesSet
	}

//	@Test def downloadedSubmissions = withFakeTime(dateTime(2001, 6)) {
//		
//	}
	
	@Test def index = withFakeTime(dateTime(2001, 6)) {
		val stopwatch = new StopWatch
		
		val jsonData = Map(
					"students" -> Array("0123456", "0199999")
				)
		val jsonDataString = json.writeValueAsString(jsonData)
		
		stopwatch.start("creating items")
		
		val defendEvents = for (i <- 1 to 1000)
			yield AuditEvent(
				id=i, eventId="d"+i, eventType="DefendBase", eventStage="before", userId="jim",
				eventDate=new DateTime(2000,1,2,0,0,0).plusSeconds(i),
				data="{}"
			)
		
		val publishEvents = for (i <- 1 to 20)
			yield AuditEvent(
				id=1000+i, eventId="s"+i, eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(i),
				data=jsonDataString
			)
		
		stopwatch.stop()
		
		val events = defendEvents ++ publishEvents
		
		service.listNewerThan(any[DateTime], isEq(1000)) returns events
		service.getById(any[JLong]) returns events.headOption
		
		stopwatch.start("indexing")
		
		indexer.index
		
		stopwatch.stop()
		
		val user = new User("jeb")
		user.setWarwickId("0123456")
		
		indexer.student(user).size should be (20)
		
		indexer.indexEvents(Seq(AuditEvent(
				id=9000, eventId="9000", eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(9000),
				data=jsonDataString
			)))
		
		indexer.student(user).size should be (21)
		
		indexer.listRecent(0, 13).size should be (13)
		
		indexer.openQuery("eventType:PublishFeedback", 0, 100).size should be (21)
			
		// First query is slowest, but subsequent queries quickly drop
		// to a much smaller time
		for (i <- 1 to 20) {
			stopwatch.start("searching for newest item forever attempt "+i)
			val newest = indexer.newest()
			stopwatch.stop()
			newest.head.getValues("id").toList.head should be ("1000")
		}
		// println(stopwatch.prettyPrint())
		
		// index again to check that it doesn't do any once-only stuff
		indexer.index
		
		there was atLeastOne(service).parseData("{}")
		there was atLeastOne(service).parseData(jsonDataString)
		
		
	}
}