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
import uk.ac.warwick.tabula.data.model.StudentRelationship

class ProfileServiceTest extends AppContextTestBase with Mockito {
	
	@Autowired var profileService:ProfileServiceImpl = _
	@Autowired var dao:MemberDao = _
	
	@Before def setup {
		profileService.memberDao = dao
	}
	
	@Transactional
	@Test def findingRelationships = withFakeTime(dateTime(2000, 6)) {
		profileService.findCurrentRelationship(PersonalTutor, "1250148/1") should be (None)
		
		profileService.saveStudentRelationship(PersonalTutor, "1250148/1", "1234567")
		
		val opt = profileService.findCurrentRelationship(PersonalTutor, "1250148/1")
		val currentRelationship = opt.getOrElse(fail("Failed to get current relationship"))
		currentRelationship.agent should be ("1234567")
		
		// now we've stored a relationship.  Try storing the identical one again:
		profileService.saveStudentRelationship(PersonalTutor, "1250148/1", "1234567")
		
		profileService.findCurrentRelationship(PersonalTutor, "1250148/1").getOrElse(fail("Failed to get current relationship after re-storing")).agent should be ("1234567")		
		profileService.getRelationships(PersonalTutor, "1250148/1").size should be (1)
		
		// now store a new personal tutor for the same student:
		profileService.saveStudentRelationship(PersonalTutor, "1250148/1", "7654321")
		
		val currentRelationshipUpdated = profileService.findCurrentRelationship(PersonalTutor, "1250148/1").getOrElse(fail("Failed to get current relationship after storing another"))
		currentRelationshipUpdated.agent should be ("7654321")		
		
		profileService.getRelationships(PersonalTutor, "1250148/1").size should be (2)
	}
}
