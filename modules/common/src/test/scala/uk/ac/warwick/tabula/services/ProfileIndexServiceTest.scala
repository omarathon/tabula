package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import org.apache.lucene.util.LuceneTestCase
import org.junit.{Test, After, Before}
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.JavaImports._
import org.joda.time.DateTime
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
import java.io.File
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.spring.Wire

import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.Fixtures
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Callable

class ProfileIndexServiceTest extends AppContextTestBase with Mockito {
	
	lazy val indexer = Wire[ProfileIndexService]
	lazy val dao = Wire[MemberDao]
	var TEMP_DIR:File = _
	
	@Before def setup {
		TEMP_DIR = createTemporaryDirectory
		indexer.dao = dao
		indexer.indexPath = TEMP_DIR
		indexer.searcherManager = null
		indexer.afterPropertiesSet
	}
	
	@Test def stripTitles {
		indexer.stripTitles("Mathew Mannion") should be ("Mathew Mannion")
		indexer.stripTitles("Mr Mathew Mannion") should be ("Mathew Mannion")
		indexer.stripTitles("Mr. Mathew Mannion") should be ("Mathew Mannion")
		indexer.stripTitles("Prof.Mathew Mannion") should be ("Mathew Mannion")
	}

	@Test def find = transactional { tx => withFakeTime(dateTime(2000, 6)) {
		val dept = Fixtures.department("CS", "Computer Science")
		session.save(dept)
		
		val m = new StudentMember
		m.universityId = "0672089"
		m.userId = "cuscav"
		m.firstName = "Mathew"
		m.fullFirstName = "Mathew James"
		m.lastName = "Mannion"
		m.homeDepartment = dept
		m.lastUpdatedDate = new DateTime(2000,1,2,0,0,0)
		m.userType = Student
		
		session.save(m)
		session.flush
		
		indexer.index
		indexer.listRecent(0, 1000).size should be (1)
		
		indexer.find("bob thornton", Seq(dept), Set(), false) should be ('empty)
		indexer.find("Mathew", Seq(dept), Set(), false).head should be (m)
		indexer.find("mat", Seq(dept), Set(), false).head should be (m)
		indexer.find("m mannion", Seq(dept), Set(), false).head should be (m)
		indexer.find("mathew james mannion", Seq(dept), Set(), false).head should be (m)
		indexer.find("mat mannion", Seq(dept), Set(), false).head should be (m)
		indexer.find("m m", Seq(dept), Set(), false).head should be (m)
		indexer.find("m m", Seq(dept), Set(Student, Staff), false).head should be (m)
		indexer.find("m m", Seq(Fixtures.department("OT", "Some other department"), dept), Set(Student, Staff), false).head should be (m)
		indexer.find("m m", Seq(Fixtures.department("OT", "Some other department")), Set(Student, Staff), false) should be ('empty)
		indexer.find("m m", Seq(dept), Set(Staff), false) should be ('empty)
	}}
	
	@Test def index = transactional { tx => withFakeTime(dateTime(2000, 6)) {
		val stopwatch = new StopWatch
		stopwatch.start("creating items")
		
		val items = for (i <- 1 to 100)
			yield {
				val m = new StudentMember
				m.universityId = i.toString
				m.userId = i.toString
				m.lastUpdatedDate = new DateTime(2000,1,2,0,0,0).plusSeconds(i)
				m.userType = Student
				
				m
			}
		stopwatch.stop()
		
		for (item <- items) dao.saveOrUpdate(item)
		session.flush
		dao.getByUniversityId("1").isDefined should be (true)
		
		dao.listUpdatedSince(new DateTime(2000,1,1,0,0,0), 10).size should be (10)
		dao.listUpdatedSince(new DateTime(1999,6,1,0,0,0), 25).size should be (25)
		
		stopwatch.start("indexing")
		
		// we only index 250 at a time, so index five times to get all the latest stuff.
		indexer.index
		indexer.index
		indexer.index
		indexer.index
		indexer.index
		
		stopwatch.stop()
		
		indexer.listRecent(0, 100).size should be (100)
				
		val moreItems = {
			val m = new StudentMember
			m.universityId = "x9000"
			m.userId = "x9000"
			m.lastUpdatedDate = new DateTime(2000,1,1,0,0,0).plusSeconds(9000)
			
			val events = Seq(m)
			events.foreach { dao.saveOrUpdate(_) }
			dao.getByUniversityId("x9000")
			
			events
		}
		indexer.indexItems(moreItems)
				
		indexer.listRecent(0, 13).size should be (13)
					
		// First query is slowest, but subsequent queries quickly drop
		// to a much smaller time
		for (i <- 1 to 20) {
			stopwatch.start("searching for newest item forever attempt "+i)
			val newest = indexer.newest()
			stopwatch.stop()
			newest.head.getValues("universityId").toList.head should be ("100")
		}
		
		// index again to check that it doesn't do any once-only stuff
		indexer.index
		
	}}
	
	@Test def threading {
		val dept = Fixtures.department("CS", "Computer Science")
		val callable = new Callable[Seq[Member]] {
			override def call() = indexer.find("mathew james mannion", Seq(dept), Set(), false)
		}
		
		// TAB-296
		val executionService = Executors.newFixedThreadPool(5)
		val cs = new ExecutorCompletionService[Seq[Member]](executionService)
		
		for (i <- 1 to 100)
			cs.submit(callable)
			
		for (i <- 1 to 100)
			cs.take().get() should be ('empty)
	}
}