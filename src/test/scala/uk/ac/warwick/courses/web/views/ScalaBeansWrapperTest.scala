package uk.ac.warwick.courses.web.views
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.junit.JUnitSuite
import org.junit.Test
import scala.collection.mutable.Buffer
import freemarker.template.SimpleSequence
import scala.reflect.BeanProperty

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
	  val list:java.util.List[String] = collection.JavaConversions.asList(Buffer("yes"))
	  wrapper.wrap(list) match {
	 	  case listy:SimpleSequence => 
	 	  case nope => fail("nope" + nope.getClass().getName())
	  }
	   
	  class ListHolder {
	 	  @BeanProperty val list:java.util.List[String] = collection.JavaConversions.asList(Buffer("contents"))
	  }
	   
	  new ListHolder().list.size should be (1)
	   
	  wrapper.wrap(new ListHolder()) match {
	 	  case hash:ScalaHashModel => {
	 	 	  hash.get("list") match {
	 	 	 	  case listy:SimpleSequence => listy.size should be (1)
	 	 	  }
	 	  }
	  }
	   
	}
}