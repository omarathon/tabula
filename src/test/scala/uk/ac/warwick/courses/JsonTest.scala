package uk.ac.warwick.courses
import org.codehaus.jackson.map.ObjectMapper
import org.junit.Test

import com.fasterxml.jackson.module.scala.DefaultScalaModule


case class Wow()

class JsonTest extends TestBase {
	// Test DefaultScalaModule from https://github.com/FasterXML/jackson-module-scala
	// which teaches Jackson about Scala collections.
	@Test def encodeMap {
		val m = new ObjectMapper()
		m.registerModule(DefaultScalaModule)
		m.writeValueAsString(Map()) should be("{}")
		
		m.writeValueAsString(Map("animals" -> Array("cat","dog"))) should be("{\"animals\":[\"cat\",\"dog\"]}")
	}
}