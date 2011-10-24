package uk.ac.warwick.courses.web.views
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.junit.JUnitSuite
import org.junit.Test

class MyObject {
  var name = "text"
  def getMotto() = "do be good, don't be bad"
}

class ScalaBeansWrapperTest extends JUnitSuite with ShouldMatchersForJUnit {
	@Test def scalaGetter {
	  val wrapper = new ScalaBeansWrapper()
	  wrapper.wrap(new MyObject) match {
	    case hash:ScalaHashModel => {
	      hash.get("name").toString should be("text")
	      hash.get("motto").toString should be("do be good, don't be bad")
	    }
	  }
	}
}