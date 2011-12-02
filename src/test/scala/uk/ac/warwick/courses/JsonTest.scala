package uk.ac.warwick.courses
import scala.collection.mutable.{Map => MutableMap}

import org.codehaus.jackson.map.ObjectMapper
import org.junit.Test

import com.fasterxml.jackson.module.scala.DefaultScalaModule

class JsonTest extends TestBase {
	
	val m = new ObjectMapper()
	m.registerModule(DefaultScalaModule)
	
	// Test DefaultScalaModule from https://github.com/FasterXML/jackson-module-scala
	// which teaches Jackson about Scala collections.
	@Test def encodeMap {
		m.writeValueAsString(Map()) should be("{}")
		
		m.writeValueAsString(Map("animals" -> Array("cat","dog"))) should be("{\"animals\":[\"cat\",\"dog\"]}")
	}
	
	// bug in scala jackson module
	@Test/*(expected=classOf[ArrayIndexOutOfBoundsException])*/ def mutableMapWorks {
		m.writeValueAsString(MutableMap()) should be("{}")
	}
}