package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import org.apache.lucene.util.LuceneTestCase
import org.junit.Test
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.JavaImports._
import org.junit.After
import org.junit.Before
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.{Assignment, AuditEvent, Submission}
import uk.ac.warwick.tabula.commands._
import org.apache.lucene.index.IndexReader
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import uk.ac.warwick.userlookup.User
import collection.JavaConversions._
import uk.ac.warwick.util.core.StopWatch
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.tabula.helpers.ArrayList
import java.io.File
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional


class AuditEventIndexServiceTest extends AppContextTestBase with Mockito {
	
	var indexer:AuditEventIndexService = _
	@Autowired var service:AuditEventService = _
	var TEMP_DIR:File = _
	
	@Before def setup {		
		TEMP_DIR = createTemporaryDirectory
		val maintenanceMode = mock[MaintenanceModeService]
		indexer = new AuditEventIndexService
		indexer.service = service
		indexer.indexPath = TEMP_DIR
		indexer.maintenanceService = maintenanceMode
		indexer.afterPropertiesSet
	}

	/**
	 * Check that when you download submissions, they are shown
	 * by adminDownloadedSubmissions(Assignment).
	 */
	@Transactional
	@Test def downloadedSubmissions = withFakeTime(dateTime(2001, 6)) {
		val assignment = {
			val a = newDeepAssignment()
			a.id = "12345"
			val s1 = new Submission
			s1.submittedDate = new DateTime().minusHours(5)
			a.submissions add s1
			a
		}
		
		val command = new NullCommand {
			override lazy val eventName = "DownloadAllSubmissions"
		  
			override def describe(d: Description) = d
				.assignment(assignment)
				.submissions(assignment.submissions)
				.studentIds(assignment.submissions.map(_.universityId))
				.properties(
						"submissionCount" -> Option(assignment.submissions).map(_.size).getOrElse(0))
		}

		val auditEvent = recordAudit(command)
		
		indexer.adminDownloadedSubmissions(assignment) should be ('empty)
		indexer.index
		indexer.adminDownloadedSubmissions(assignment) should be (assignment.submissions.toList)
	}
	
	def recordAudit(command:Command[_]) = {
		val event = Event.fromDescribable(command)
		service.save(event, "before")
		service.getByEventId(event.id)
	}
	
	def addParsedData(event:AuditEvent) = {
		event.parsedData = service.parseData(event.data)
		event
	}

	@Transactional
	@Test def createdDate = withFakeTime(dateTime(2000, 6)) {

		val eventId = "a"
		val eventType = "AddAssignment"
		val userId = "bob"
		val d = new DateTime(2000,1,1,0,0,0)

		val before = AuditEvent(
			eventId=eventId, eventType=eventType, userId=userId, eventDate=d,
			eventStage="before", data="""{}"""
		)
		val after = AuditEvent(
			eventId=eventId, eventType=eventType, userId=userId, eventDate=d,
			eventStage="after", data="""{"assignment":"12345"}"""
		)

		for (event <- Seq(before,after)) service.save(addParsedData(event))
		//indexer.indexEvents(Seq(before))
		indexer.index

		val assignment = new Assignment()
		assignment.id = "12345"

		val maybeDate = indexer.getAssignmentCreatedDate(assignment)
		if (maybeDate.isEmpty) fail("No date found")
		else for (date <- maybeDate) date should be (d)

	}
	
	@Transactional
	@Test def index = withFakeTime(dateTime(2000, 6)) {
		val stopwatch = new StopWatch
		
		val jsonData = Map(
					"students" -> Array("0123456", "0199999")
				)
		val jsonDataString = json.writeValueAsString(jsonData)
		
		stopwatch.start("creating items")
		
		val defendEvents = for (i <- 1 to 1000)
			yield AuditEvent(
				eventId="d"+i, eventType="DefendBase", eventStage="before", userId="jim",
				eventDate=new DateTime(2000,1,2,0,0,0).plusSeconds(i),
				data="{}"
			)
		
		val publishEvents = for (i <- 1 to 20)
			yield AuditEvent( 
				eventId="s"+i, eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(i),
				data=jsonDataString
			)
		
		stopwatch.stop()
		
		val events = defendEvents ++ publishEvents
		events.foreach { event =>
			service.save(addParsedData(event))
		}
		
		service.listNewerThan(new DateTime(2000,1,1,0,0,0), 100).size should be (100)
		
		stopwatch.start("indexing")
		
		// we only index 1000 at a time, so index twice to get all the latest stuff.
		indexer.index
		indexer.index
		
		stopwatch.stop()
		
		val user = new User("jeb")
		user.setWarwickId("0123456")
		
		indexer.listRecent(0, 1000).size should be (1000)
		
		indexer.student(user).size should be (20)
		
		val moreEvents = {
			val events = Seq(addParsedData(AuditEvent(
				eventId="x9000", eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(9000),
				data=jsonDataString
			)))
			events.foreach { service.save(_) }
			service.getByEventId("x9000")
		}
		indexer.indexEvents(moreEvents)
		
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
		
	}
}