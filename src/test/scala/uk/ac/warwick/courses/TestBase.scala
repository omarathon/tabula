package uk.ac.warwick.courses

import java.io.File
import java.util.Properties
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matcher
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.ReadableInstant
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit
import org.specs.mock.JMocker._
import org.specs.mock.JMocker.{expect => expecting}
import org.specs.mock.JMocker.`with`
import org.springframework.core.io.ClassPathResource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.userlookup.User
import org.junit.After
import uk.ac.warwick.util.core.spring.FileUtils

trait TestBase extends JUnitSuite with ShouldMatchersForJUnit {
  
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
	  file.mkdir() should be (true)
	  temporaryDirectories += file
	  file
  }
  
  /**
   * Removes any directories created by #createTemporaryDirectory
   */
  //@After def deleteTemporaryDirs = temporaryDirectories.par.foreach( FileUtils.recursiveDelete _ )
	
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
	   
  def dateTime(year:Int, month:Int) = new DateTime(year,month,1,0,0,0)
  
  var currentUser:CurrentUser = null
  
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
  
  def resourceAsBytes(path:String):Array[Byte] = FileCopyUtils.copyToByteArray(new ClassPathResource(path).getInputStream)
  
  def emptyFeatures = new Features(new Properties)

  def testRequest(uri:String=null) = {
    val req = new MockHttpServletRequest
    req.setRequestURI(uri)
    req
  }
  
  def testResponse = new MockHttpServletResponse
}