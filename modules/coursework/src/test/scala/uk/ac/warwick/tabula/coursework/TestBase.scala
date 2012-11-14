package uk.ac.warwick.tabula.coursework

import java.io.File
import java.util.Properties
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matcher
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.ReadableInstant
import org.junit.After
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit
import org.specs.mock.JMocker._
import org.specs.mock.JMocker.{ expect => expecting }
import org.specs.mock.JMocker.`with`
import org.springframework.core.io.ClassPathResource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.util.FileCopyUtils
import collection.JavaConversions._
import freemarker.cache.ClassTemplateLoader
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfiguration



import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.core.spring.FileUtils
import freemarker.cache.MultiTemplateLoader
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.junit.Before
import java.io.StringWriter
import java.io.StringReader
import org.aspectj.lang.{NoAspectBoundException, Aspects}
import uk.ac.warwick.tabula.coursework.data.model.Department
import uk.ac.warwick.tabula.coursework.data.model.Module
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}

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
trait TestFixtures extends uk.ac.warwick.tabula.TestFixtures {
	def newFreemarkerConfiguration = new ScalaFreemarkerConfiguration {
		setTemplateLoader(new MultiTemplateLoader(Array(
			new ClassTemplateLoader(getClass, "/freemarker/"), // to match test templates
			new ClassTemplateLoader(getClass, "/") // to match live templates
			)))
		setAutoIncludes(Nil) // don't use prelude
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
}

trait TestHelpers extends uk.ac.warwick.tabula.TestHelpers {
	lazy val json = new JsonObjectMapperFactory().createInstance

	def readJsonMap(s: String): Map[String, Any] = json.readValue(new StringReader(s), classOf[java.util.Map[String, Any]]).toMap

	var currentUser: CurrentUser = null

	/** Sets up a pretend requestinfo context with the given pretend user
	  * around the callback.
	  *
	  * withUser("cusebr") { /* ... your code */  }
	  */
	def withUser(code: String, universityId: String = null)(fn: => Unit) {
		val requestInfo = RequestInfo.fromThread match {
			case Some(info) => throw new IllegalStateException("A RequestInfo is already open")
			case None => {
				val user = new User(code)
				user.setIsLoggedIn(true)
				user.setWarwickId(universityId)
				currentUser = new CurrentUser(user, user)
				new RequestInfo(currentUser, null)
			}
		}

		try {
			RequestInfo.open(requestInfo)
			fn
		} finally {
			currentUser = null
			RequestInfo.close
		}
	}
	
}