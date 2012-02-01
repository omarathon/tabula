package uk.ac.warwick.courses
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matcher
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.ReadableInstant
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit
import org.specs.mock.JMocker.`with`
import uk.ac.warwick.userlookup.User
import org.springframework.util.FileCopyUtils
import org.springframework.core.io.ClassPathResource
import org.specs.mock.JMocker._
import org.specs.mock.JMocker.{expect => expecting}
import uk.ac.warwick.userlookup.UserLookupInterface
import java.util.Properties

trait TestBase extends JUnitSuite with ShouldMatchersForJUnit {
  
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
  
}