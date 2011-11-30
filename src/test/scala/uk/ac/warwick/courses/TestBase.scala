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
  
  def withUser(code:String)(fn: =>Unit) {
	  val requestInfo = RequestInfo.fromThread match {
	 	  case Some(info) => throw new IllegalStateException("A RequestInfo is already open")
	 	  case None => new RequestInfo(new CurrentUser(new User(code), false))
	  }
	   
	  try {
	 	  RequestInfo.open(requestInfo)
	 	  fn
	  } finally {
	 	  RequestInfo.close
	  }
  } 
  
}