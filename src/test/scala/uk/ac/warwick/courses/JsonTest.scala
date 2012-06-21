package uk.ac.warwick.courses
import scala.collection.mutable.{Map => MutableMap}
import org.codehaus.jackson.map.ObjectMapper
import org.junit.Test
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.io.StringReader
import collection.JavaConversions._
import uk.ac.warwick.courses.JavaImports._

class JsonTest extends TestBase {
	
	val m = new ObjectMapper()
	m.registerModule(DefaultScalaModule)
	
	// Test DefaultScalaModule from https://github.com/FasterXML/jackson-module-scala
	// which teaches Jackson about Scala collections.
	@Test def encodeMap {
		m.writeValueAsString(Map()) should be("{}")
		
		m.writeValueAsString(Map("animals" -> Array("cat","dog"))) should be("{\"animals\":[\"cat\",\"dog\"]}")
		
//		m.writeValueAsString(Map("animals" -> List("cat","dog"))) should be("{\"animals\":[\"cat\",\"dog\"]}")
	}
	
	@Test def parseNumbers {
		val props = """{"age" : 23, "filetypes":["pdf","doc","docx"]}"""
		val map = m.readValue(new StringReader(props), classOf[Map[String,Any]])
		map("filetypes").asInstanceOf[JList[_]].toSeq should equal (Seq("pdf","doc","docx"))
		map("age").asInstanceOf[Int] should be (23)
	}
	
	// bug in scala jackson module
	@Test/*(expected=classOf[ArrayIndexOutOfBoundsException])*/ def mutableMapWorks {
		m.writeValueAsString(MutableMap()) should be("{}")
	}
}