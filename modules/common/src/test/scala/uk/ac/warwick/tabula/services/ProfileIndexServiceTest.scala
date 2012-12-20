package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import org.apache.lucene.util.LuceneTestCase
import org.junit.Test
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.JavaImports._
import org.junit.After
import org.junit.Before
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
import uk.ac.warwick.tabula.helpers.ArrayList
import java.io.File
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.AppContextTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model.Member
import java.lang.Boolean
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.Student
import uk.ac.warwick.tabula.data.model.Staff
import uk.ac.warwick.tabula.Fixtures

class ProfileIndexServiceTest extends AppContextTestBase with Mockito {
	
	var indexer:ProfileIndexService = _
	@Autowired var dao:MemberDao = _
	var TEMP_DIR:File = _
	
	@Before def setup {
		TEMP_DIR = createTemporaryDirectory
		val maintenanceMode = mock[MaintenanceModeService]
		indexer = new ProfileIndexService
		indexer.dao = dao
		indexer.indexPath = TEMP_DIR
		indexer.searcherManager = null
		indexer.maintenanceService = maintenanceMode
		indexer.afterPropertiesSet
	}
	
	@Test def stripTitles {
		indexer.stripTitles("Mathew Mannion") should be ("Mathew Mannion")
		indexer.stripTitles("Mr Mathew Mannion") should be ("Mathew Mannion")
		indexer.stripTitles("Mr. Mathew Mannion") should be ("Mathew Mannion")
		indexer.stripTitles("Prof.Mathew Mannion") should be ("Mathew Mannion")
	}
	
	@Transactional
	@Test def find = withFakeTime(dateTime(2000, 6)) {
		val dept = Fixtures.department("CS", "Computer Science")
		
		val m = new Member
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
		
		indexer.find("bob thornton", Seq(dept), Set()) should be ('empty)
		indexer.find("Mathew", Seq(dept), Set()).head should be (m)
		indexer.find("mat", Seq(dept), Set()).head should be (m)
		indexer.find("m mannion", Seq(dept), Set()).head should be (m)
		indexer.find("mathew james mannion", Seq(dept), Set()).head should be (m)
		indexer.find("mat mannion", Seq(dept), Set()).head should be (m)
		indexer.find("m m", Seq(dept), Set()).head should be (m)
		indexer.find("m m", Seq(dept), Set(Student, Staff)).head should be (m)
		indexer.find("m m", Seq(Fixtures.department("OT", "Some other department"), dept), Set(Student, Staff)).head should be (m)
		indexer.find("m m", Seq(Fixtures.department("OT", "Some other department")), Set(Student, Staff)) should be ('empty)
		indexer.find("m m", Seq(dept), Set(Staff)) should be ('empty)
	}
	
	@Transactional
	@Test def index = withFakeTime(dateTime(2000, 6)) {
		val stopwatch = new StopWatch
		stopwatch.start("creating items")
		
		val items = for (i <- 1 to 1000)
			yield {
				val m = new Member
				m.universityId = i.toString
				m.userId = i.toString
				m.lastUpdatedDate = new DateTime(2000,1,2,0,0,0).plusSeconds(i)
				m.userType = Student
				
				m
			}
		stopwatch.stop()
		
		for (item <- items) dao.saveOrUpdate(item)
		session.flush
		dao.getByUniversityId("1").isDefined should be (Boolean.TRUE)
		
		dao.listUpdatedSince(new DateTime(2000,1,1,0,0,0), 100).size should be (100)
		dao.listUpdatedSince(new DateTime(1999,6,1,0,0,0), 250).size should be (250)
		
		stopwatch.start("indexing")
		
		// we only index 250 at a time, so index five times to get all the latest stuff.
		indexer.index
		indexer.index
		indexer.index
		indexer.index
		indexer.index
		
		stopwatch.stop()
		
		indexer.listRecent(0, 1000).size should be (1000)
				
		val moreItems = {
			val m = new Member
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
			newest.head.getValues("universityId").toList.head should be ("1000")
		}
		println(stopwatch.prettyPrint())
		
		// index again to check that it doesn't do any once-only stuff
		indexer.index
		
	}
}