package uk.ac.warwick.courses
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.junit.JUnitSuite
import org.specs.mock.JMocker.`with`
import org.hamcrest.Matchers._
import org.hamcrest.Matcher
import org.scalatest.BeforeAndAfter
import org.specs.specification.DefaultExampleExpectationsListener
import org.joda.time.ReadableInstant
import org.joda.time.DateTimeUtils
import org.joda.time.DateTime

trait TestBase extends JUnitSuite with ShouldMatchersForJUnit {
  
  /**
   * withArgs(a,b,c) translates to
   * with(allOf(a,b,c)).
   * 
   * :_* is used to pass varargs from one function to another
   * function that also takes varargs. 
   */
  def withArg[T](matcher:Matcher[T]*) = `with`( allOf( matcher:_* ) )

  def withFakeTime(when:ReadableInstant)(fn:()=>Unit) = 
	  try {
	 	  DateTimeUtils.setCurrentMillisFixed(when.getMillis)
	 	  fn()
	  } finally {
	 	  DateTimeUtils.setCurrentMillisSystem
	  }
	   
  def dateTime(year:Int, month:Int) = new DateTime(year,month,1,0,0,0)
  
}