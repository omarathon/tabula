package uk.ac.warwick.tabula

import java.io.File
import java.io.StringReader

import scala.collection.JavaConversions._

import org.apache.commons.configuration.PropertiesConfiguration
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.ReadableInstant
import org.junit.After
import org.junit.Before
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit
import org.specs.mock.JMocker._
import org.specs.mock.JMocker.`with`
import org.springframework.core.io.ClassPathResource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.util.FileCopyUtils

import freemarker.cache.ClassTemplateLoader
import freemarker.cache.MultiTemplateLoader
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfiguration
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.core.spring.FileUtils
import uk.ac.warwick.util.web.Uri

/** Base class for tests which boringly uses the JUnit support of
  * Scalatest, so you do @Test annotated methods as you normally would.
  * You can use ScalaTest's "should" matchers though, which is nice.
  *
  * Also a bunch of methods for generating fake support resources.
  */
abstract class TestBase extends JUnitSuite with ShouldMatchersForJUnit with TestHelpers with TestFixtures {
	// bring in type so we can be lazy and not have to import @Test
	type Test = org.junit.Test 
}

/** Various test objects
  */
trait TestFixtures {
	def newFreemarkerConfiguration = new ScalaFreemarkerConfiguration {
		setTemplateLoader(new MultiTemplateLoader(Array(
			new ClassTemplateLoader(getClass, "/freemarker/"), // to match test templates
			new ClassTemplateLoader(getClass, "/") // to match live templates
			)))
		setAutoIncludes(Nil) // don't use prelude
	}
	
	def testRequest(uri: String = null) = {
		val req = new MockHttpServletRequest
		req.setRequestURI(uri)
		req
	}
	
	def emptyFeatures = Features.empty

	/** Creates an Assignment with a module and department,
	  * and a few pre-filled fields. 
	  */
	def newDeepAssignment(moduleCode: String="IN101") = {
		val department = new Department
		val module = new Module(moduleCode, department)
		new Assignment(module)
	}

	def testResponse = new MockHttpServletResponse

	/** Returns midnight on the first day of this year and month. */
	def dateTime(year: Int, month: Int): DateTime = dateTime(year, month, 1)
	def dateTime(year: Int, month: Int, day: Int): DateTime = new DateTime(year, month, day, 0, 0, 0, 0)
	
	def newSSOConfiguration = {
		val config = new PropertiesConfiguration()
		config.addProperty("mode", "new")
    config.addProperty("origin.login.location", "https://xebsignon.warwick.ac.uk/origin/hs")
    config.addProperty("shire.location", "https://xabula.warwick.ac.uk/tabula/shire")
    config.addProperty("shire.providerid", "tabula:service")
    
    new SSOConfiguration(config)
	}
}

trait TestHelpers extends TestFixtures {
	lazy val json = new JsonObjectMapperFactory().createInstance

	def readJsonMap(s: String): Map[String, Any] = json.readValue(new StringReader(s), classOf[JMap[String, Any]]).toMap

	var currentUser: CurrentUser = null
  
	var temporaryFiles: Set[File] = Set.empty

	// Location of /tmp - best to create a subdir below it.
	lazy val IoTmpDir = new File(System.getProperty("java.io.tmpdir"))
	val random = new scala.util.Random

	@Before def emptyTempDirSet = temporaryFiles = Set.empty

	@Before def setupAspects = {
		
	}

	/** Returns a new temporary directory that will get cleaned up
	  * automatically at the end of the test.
	  */
	def createTemporaryDirectory: File = {
		// try 10 times to find an unused filename.
		// Stream is lazy so it won't try making 10 files every time.
		val dir = findTempFile
		if (!dir.mkdir()) throw new IllegalStateException("Couldn't create " + dir)
		temporaryFiles += dir
		dir
	}

	def createTemporaryFile: File = {
		val file = findTempFile
		if (!file.createNewFile()) throw new IllegalStateException("Couldn't create " + file)
		temporaryFiles += file
		file
	}

	private def findTempFile: File = {
		def randomTempFile() = new File(IoTmpDir, "JavaTestTmp-" + random.nextLong())
		
		// Create a Stream that will generate random files forever, then take the first 10.
		// The Iterator will only calculate its elements on demand so it won't always generate 10 Files. 
		Iterator.continually( randomTempFile ).take(10)
			.find(!_.exists)
			.getOrElse(throw new IllegalStateException("Couldn't find unique filename!"))
	}

	/** Removes any directories created by #createTemporaryDirectory
	  */
	@After def deleteTemporaryDirs = try{temporaryFiles.par foreach FileUtils.recursiveDelete} catch {case _: Throwable => /* squash! will be cleaned from temp eventually anyway */}

	/** withArgs(a,b,c) translates to
	  * with(allOf(a,b,c)).
	  *
	  * :_* is used to pass varargs from one function to another
	  * function that also takes varargs.
	  */
	def withArg[A](matcher: Matcher[A]*) = `with`(allOf(matcher: _*))

	def withFakeTime(when: ReadableInstant)(fn: => Unit) =
		try {
			DateTimeUtils.setCurrentMillisFixed(when.getMillis)
			fn
		} finally {
			DateTimeUtils.setCurrentMillisSystem
		}
		
	/** Sets up a pretend requestinfo context with the given pretend user
	  * around the callback. 
	  * 
	  * Can pass null as the usercode to make an anonymous user.
	  *
	  * withUser("cusebr") { /* ... your code */  }
	  */
	def withUser(code: String, universityId: String = null)(fn: => Unit) {
		val user = if (code == null) {
			new AnonymousUser()
		} else {
			val u = new User(code)
			u.setIsLoggedIn(true)
			u.setFoundUser(true)
			u.setWarwickId(universityId)
			u
		}
		
		withCurrentUser(new CurrentUser(user, user))(fn)
	}
	
	def withCurrentUser(user: CurrentUser)(fn: => Unit) {
		val requestInfo = RequestInfo.fromThread match {
			case Some(info) => throw new IllegalStateException("A RequestInfo is already open")
			case None => {
				new RequestInfo(user, Uri.parse("http://www.example.com/page"), Map())
			}
		}

		try {
			currentUser = user
			RequestInfo.open(requestInfo)
			fn
		} finally {
			currentUser = user
			RequestInfo.close
		}
	}
	
	def withSSOConfig(ssoConfig: SSOConfiguration = newSSOConfiguration)(fn: => Unit) {
		try {
			SSOConfiguration.setConfig(ssoConfig)
			fn
		} finally {
			SSOConfiguration.setConfig(null)
		}
	}

	/** Fetches a resource as a string. Assumes UTF-8 unless specified.
	  */
	def resourceAsString(path: String, encoding: String = "UTF-8"): String = new String(resourceAsBytes(path), encoding)
	def resourceAsBytes(path: String): Array[Byte] = FileCopyUtils.copyToByteArray(new ClassPathResource(path).getInputStream)

	
	def containMatching[A](f: (A)=>Boolean) = org.scalatest.matchers.Matcher[Seq[A]] { (v:Seq[A]) =>
		org.scalatest.matchers.MatchResult(
    		v exists f,
    		"Contained a matching value",
    		"Contained no matching value"
		)
	}
	
}
