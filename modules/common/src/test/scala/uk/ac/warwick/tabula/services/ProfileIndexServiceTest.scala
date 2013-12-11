package uk.ac.warwick.tabula.services

import java.io.File
import java.util.concurrent.Executors

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import org.junit.{After, Before}
import org.springframework.transaction.annotation.Transactional
import org.scalatest.concurrent.AsyncAssertions

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures, Mockito}
import uk.ac.warwick.tabula.data.MemberDaoImpl
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.core.StopWatch
import scala.concurrent.duration.Duration

// scalastyle:off magic.number
class ProfileIndexServiceTest extends PersistenceTestBase with Mockito with Logging with AsyncAssertions {

	val indexer:ProfileIndexService = new ProfileIndexService
	val dao = new MemberDaoImpl
	var TEMP_DIR:File = _

	@Before def setup {
		TEMP_DIR = createTemporaryDirectory
		dao.sessionFactory = sessionFactory
		indexer.dao = dao
		indexer.indexPath = TEMP_DIR
		indexer.searcherManager = null
		indexer.afterPropertiesSet()
	}

	@After def tearDown {
		indexer.destroy()
		try {
			FileUtils.deleteDirectory(TEMP_DIR)
		} catch {
			// windows holds onto a lock on some index files longer than it should. Don't fail over this
			case e:java.io.IOException => logger.warn(e.getMessage)
		}
	}

	@Test def stripTitles {
		indexer.stripTitles("Mathew Mannion") should be ("Mathew Mannion")
		indexer.stripTitles("Mr Mathew Mannion") should be ("Mathew Mannion")
		indexer.stripTitles("Mr. Mathew Mannion") should be ("Mathew Mannion")
		indexer.stripTitles("Prof.Mathew Mannion") should be ("Mathew Mannion")
	}

	@Test def sanitise {
		indexer.sanitise("//x/y/") should be ("\\/\\/x\\/y\\/")
		indexer.sanitise("Prof.Mathew Mannion/Mat Mannion") should be ("Mathew Mannion\\/Mat Mannion")
	}

	@Transactional
	@Test def find = withFakeTime(dateTime(2000, 6)) {
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
		m.inUseFlag = "Active"

		session.save(m)
		session.flush

		indexer.incrementalIndex
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
	}

	@Transactional
	@Test def findCopesWithApostrophes = withFakeTime(dateTime(2000, 6)) {
		val dept = Fixtures.department("CS", "Computer Science")
		session.save(dept)

		val m = new StudentMember
		m.universityId = "0000001"
		m.userId = "helpme"
		m.firstName = "Johnny"
		m.fullFirstName = "Jonathan"
		m.lastName = "O'Connell"
		m.homeDepartment = dept
		m.lastUpdatedDate = new DateTime(2000,1,2,0,0,0)
		m.userType = Student
		m.inUseFlag = "Active"

		session.save(m)
		session.flush

		indexer.incrementalIndex
		indexer.listRecent(0, 1000).size should be (1)

		indexer.find("bob thornton", Seq(dept), Set(), false) should be ('empty)
		indexer.find("joconnell", Seq(dept), Set(), false) should be ('empty)
		indexer.find("johnny connell", Seq(dept), Set(), false).head should be (m)
		indexer.find("johnny o'connell", Seq(dept), Set(), false).head should be (m)
		indexer.find("j o connell", Seq(dept), Set(), false).head should be (m)
		indexer.find("j oconnell", Seq(dept), Set(), false).head should be (m)
		indexer.find("j o'c", Seq(dept), Set(), false).head should be (m)
		indexer.find("j o c", Seq(dept), Set(), false).head should be (m)
	}

	@Transactional
	@Test def asciiFolding = withFakeTime(dateTime(2000, 6)) {
		val dept = Fixtures.department("CS", "Computer Science")
		session.save(dept)

		val m = new StudentMember
		m.universityId = "1300623"
		m.userId = "smrlar"
		m.firstName = "Aist\u0117"
		m.fullFirstName = "Aist\u0117"
		m.lastName = "Kiltinavi\u010Di\u016Ba"
		m.homeDepartment = dept
		m.lastUpdatedDate = new DateTime(2000,1,2,0,0,0)
		m.userType = Student
		m.inUseFlag = "Active"

		session.save(m)
		session.flush

		indexer.incrementalIndex
		indexer.listRecent(0, 1000).size should be (1)

		indexer.find("bob thornton", Seq(dept), Set(), false) should be ('empty)
		indexer.find("Aist\u0117", Seq(dept), Set(), false).head should be (m)
		indexer.find("aist", Seq(dept), Set(), false).head should be (m)
		indexer.find("a kiltinavi\u010Di\u016Ba", Seq(dept), Set(), false).head should be (m)
		indexer.find("aiste kiltinavi\u010Di\u016Ba", Seq(dept), Set(), false).head should be (m)
		indexer.find("aiste kiltinaviciua", Seq(dept), Set(), false).head should be (m)
	}

	@Transactional
	@Test def index = withFakeTime(dateTime(2000, 6)) {
		val stopwatch = new StopWatch
		stopwatch.start("creating items")

		val items = for (i <- 1 to 100)
			yield {
				val m = new StudentMember
				m.universityId = i.toString
				m.userId = i.toString
				m.lastUpdatedDate = new DateTime(2000,1,2,0,0,0).plusSeconds(i)
				m.userType = Student
				m.inUseFlag = "Active"

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
		indexer.incrementalIndex
		indexer.incrementalIndex
		indexer.incrementalIndex
		indexer.incrementalIndex
		indexer.incrementalIndex

		stopwatch.stop()

		indexer.listRecent(0, 100).size should be (100)

		val moreItems = {
			val m = new StudentMember
			m.universityId = "x9000"
			m.userId = "x9000"
			m.lastUpdatedDate = new DateTime(2000,1,1,0,0,0).plusSeconds(9000)
			m.inUseFlag = "Active"

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
		indexer.incrementalIndex

	}

	// TAB-296
	@Test def threading {
		val ThreadCount = 100
		val timeout = Duration(10, SECONDS)
		val dept = Fixtures.department("CS", "Computer Science")

		implicit val executionService = ExecutionContext.fromExecutor( Executors.newFixedThreadPool(5) )

		val futures = for (i <- 1 to ThreadCount) yield future {
			indexer.find("mathew james mannion", Seq(dept), Set(), false)
		}

		for (future <- futures) {
			Await.result(future, timeout) should be ('empty)
		}
	}
}