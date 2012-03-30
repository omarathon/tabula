package uk.ac.warwick.courses

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
import org.specs.mock.JMocker.{expect => expecting}
import org.specs.mock.JMocker.`with`
import org.springframework.core.io.ClassPathResource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.util.FileCopyUtils
import collection.JavaConversions._
import freemarker.cache.ClassTemplateLoader
import uk.ac.warwick.courses.web.views.ScalaFreemarkerConfiguration
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.core.spring.FileUtils
import freemarker.cache.MultiTemplateLoader
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Base class for tests which boringly uses the JUnit support of
 * Scalatest, so you do @Test annotated methods as you normally would.
 * You can use ScalaTest's "should" matchers though, which is nice.
 * 
 * Also a bunch of methods for generating fake support resources.
 */

trait TestBase extends JUnitSuite with ShouldMatchersForJUnit with TestHelpers
  
trait TestHelpers {
  var temporaryDirectories:Set[File] = Set.empty
	
  // Location of /tmp - best to create a subdir below it.
  lazy val IoTmpDir = new File(System.getProperty("java.io.tmpdir"))
  val random = new scala.util.Random
  lazy val json = new JsonObjectMapperFactory().createInstance
  
  /**
   * Returns a new temporary directory that will get cleaned up
   * automatically at the end of the test.
   */
  def createTemporaryDirectory:File = {
	  val file = new File(IoTmpDir, "JavaTestTmp-"+random.nextInt(99999999))
	  if (!file.mkdir()) throw new IllegalStateException("Couldn't create " + file)
	  temporaryDirectories += file
	  file
  }
  
  /**
   * Removes any directories created by #createTemporaryDirectory
   */
  @After def deleteTemporaryDirs = temporaryDirectories.par.foreach( FileUtils.recursiveDelete _ )
	
  /**
   * withArgs(a,b,c) translates to
   * with(allOf(a,b,c)).
   * 
   * :_* is used to pass varargs from one function to another
   * function that also takes varargs. 
   */
  def withArg[T](matcher:Matcher[T]*) = `with`( allOf( matcher:_* ) )

  def withFakeTime(when:ReadableInstant)(fn: =>Unit) = 
	  try {
	 	  DateTimeUtils.setCurrentMillisFixed(when.getMillis)
	 	  fn
	  } finally {
	 	  DateTimeUtils.setCurrentMillisSystem
	  }
	   
  /** Returns midnight on the first day of this year and month. */
  def dateTime(year:Int, month:Int) = new DateTime(year,month,1,0,0,0)
  
  var currentUser:CurrentUser = null
  
  /**
   * Sets up a pretend requestinfo context with the given pretend user
   * around the callback.
   * 
   * withUser("cusebr") { /* ... your code */  }
   */
  def withUser(code:String)(fn: =>Unit) {
	  val requestInfo = RequestInfo.fromThread match {
	 	  case Some(info) => throw new IllegalStateException("A RequestInfo is already open")
	 	  case None => {
	 	 	  val user = new User(code)
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
  
  /**
   * Fetches a resource as a string. Assumes UTF-8 unless specified.
   */
  def resourceAsString(path:String, encoding:String="UTF-8"):String = new String(resourceAsBytes(path), encoding)
  def resourceAsBytes(path:String):Array[Byte] = FileCopyUtils.copyToByteArray(new ClassPathResource(path).getInputStream)
  
  def emptyFeatures = new Features(new Properties)

  def testRequest(uri:String=null) = {
    val req = new MockHttpServletRequest
    req.setRequestURI(uri)
    req
  }
  
  def testResponse = new MockHttpServletResponse
  
  def newFreemarkerConfiguration = new ScalaFreemarkerConfiguration {
		setTemplateLoader(new MultiTemplateLoader(Array(
				new ClassTemplateLoader(getClass, "/freemarker/"), // to match test templates
				new ClassTemplateLoader(getClass, "/") // to match live templates
		)))
		setAutoIncludes(Nil) // don't use prelude
	}
}