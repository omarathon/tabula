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
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Callable
import uk.ac.warwick.tabula.data.model.PersonalTutor
import uk.ac.warwick.tabula.data.model.MemberRelationship

class ProfileServiceTest extends AppContextTestBase with Mockito {
	
	@Autowired var ps:ProfileServiceImpl = _
	@Autowired var dao:MemberDao = _
	
	@Before def setup {
		ps.memberDao = dao
	}
	
	@Transactional
	@Test def findRelationship = withFakeTime(dateTime(2000, 6)) {
		ps.findRelationship(PersonalTutor, "0070790") should be (None)
		
		//val mr = new MemberRelationship("1234567", PersonalTutor, "0070790")
		val mr = new MemberRelationship
		mr.init("1234567", PersonalTutor, "0070790")
		
		session.save(mr)
		
		ps.findRelationship(PersonalTutor, "0070790").getOrElse(fail).agent should be ("1234567")
		
	}
}
