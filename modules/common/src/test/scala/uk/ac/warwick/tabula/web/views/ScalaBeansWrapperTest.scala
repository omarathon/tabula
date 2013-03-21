package uk.ac.warwick.tabula.web.views

import scala.collection.mutable.Buffer
import scala.reflect.BeanProperty

import org.junit.Test
import org.scalatest.junit.JUnitSuite

import org.scalatest.junit.ShouldMatchersForJUnit

import freemarker.template.SimpleSequence

class MyObject {
  var name = "text"
  def getMotto() = "do be good, don't be bad"
  def grotto = "Santa's"
	  
  def getGreeting(name:String) = "Hello %s!" format (name)
  def getGreeting():String = getGreeting("you")
}

object World {
	object England {
		val plant = "Rose"
	}
	object Scotland {
		def plant = "Thistle"
	}
}

class ScalaBeansWrapperTest extends JUnitSuite with ShouldMatchersForJUnit {
	
	@Test def nestedObjects {
		World.Scotland.plant should be ("Thistle")

		val wrapper = new ScalaBeansWrapper()
		wrapper.wrap(World) match {
			case hash:ScalaHashModel => {
				hash.get("Scotland") match {
					case hash:ScalaHashModel => {
						hash.get("plant").toString should be ("Thistle")
					}
				}
			}
			case somethingElse => fail("unexpected match; expected hash:ScalaHashModel but was a " + somethingElse + ":" + somethingElse.getClass.getSimpleName)
		}
	}
	
	/**
	 * def getGreeting(name:String="you") should be able to access the
	 * default no-param version as if it were a regular getGreeting() getter.
	 */
	@Test def defaultParameters {
		val wrapper = new ScalaBeansWrapper()
		wrapper.wrap(new MyObject) match {
			case hash:ScalaHashModel => {
				hash.get("greeting").toString should be ("Hello you!")
			}
			case somethingElse => fail("unexpected match; expected hash:ScalaHashModel but was a " + somethingElse + ":" + somethingElse.getClass.getSimpleName)
		}
	}
	
	@Test def scalaGetter {
	  val wrapper = new ScalaBeansWrapper()
	  wrapper.wrap(new MyObject) match {
	    case hash:ScalaHashModel => {
	      hash.get("name").toString should be("text")
	      hash.get("motto").toString should be("do be good, don't be bad")
	      hash.get("grotto").toString should be("Santa's")
	    }
	  }
	  val list:java.util.List[String] = collection.JavaConversions.bufferAsJavaList(Buffer("yes","yes"))
	  wrapper.wrap(list) match {
	 	  case listy:SimpleSequence => 
	 	  case nope => fail("nope" + nope.getClass().getName())
	  }
	   
	  class ListHolder {
	 	  @BeanProperty val list:java.util.List[String] = collection.JavaConversions.bufferAsJavaList(Buffer("contents","bontents"))
	  }
	   
	  new ListHolder().list.size should be (2)
	   
	  wrapper.wrap(new ListHolder()) match {
	 	  case hash:ScalaHashModel => {
	 	 	  hash.get("list") match {
	 	 	 	  case listy:SimpleSequence => listy.size should be (2)
	 	 	 	  case somethingElse => fail("unexpected match; expected listy:SimpleSequence but was a " + somethingElse + ":" + somethingElse.getClass.getSimpleName)
	 	 	  }
	 	  }
	  }
	   
	}
}