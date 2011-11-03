package uk.ac.warwick.courses
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.junit.JUnitSuite
import org.specs.mock.JMocker.`with`
import org.hamcrest.Matchers._
import org.hamcrest.Matcher
import org.scalatest.BeforeAndAfter
import org.specs.specification.DefaultExampleExpectationsListener

trait TestBase extends JUnitSuite with ShouldMatchersForJUnit {
  
  /**
   * withArgs(a,b,c) translates to
   * with(allOf(a,b,c)).
   * 
   * :_* is used to pass varargs from one function to another
   * function that also takes varargs. 
   */
  def withArg[T](matcher:Matcher[T]*) = `with`( allOf( matcher:_* ) )

}