package uk.ac.warwick.tabula
import scala.collection.mutable.{Map => MutableMap}
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.io.StringReader
import collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._

class JsonTest extends TestBase {
	
	val m = new ObjectMapper()
	m.registerModule(DefaultScalaModule)
	
	// Test DefaultScalaModule from https://github.com/FasterXML/jackson-module-scala
	// which teaches Jackson about Scala collections.
	@Test def encodeMap {
		m.writeValueAsString(Map()) should be("{}")
		
		m.writeValueAsString(Map("animals" -> Array("cat","dog"))) should be("""{"animals":["cat","dog"]}""")
		m.writeValueAsString(Map("animals" -> JArrayList("cat","dog"))) should be("""{"animals":["cat","dog"]}""")
		
//		m.writeValueAsString(Map("animals" -> List("cat","dog"))) should be("{\"animals\":[\"cat\",\"dog\"]}")
	}
	
	@Test def parseNumbers {
		val props = """{"age" : 23, "filetypes":["pdf","doc","docx"]}"""
		val map = m.readValue(new StringReader(props), classOf[Map[String,Any]])
		map("filetypes").asInstanceOf[scala.collection.mutable.Buffer[_]].toSeq should equal (Seq("pdf","doc","docx"))
		map("age").asInstanceOf[Int] should be (23)
	}
	
	// was a bug in scala jackson module
	@Test def mutableMapWorks {
		m.writeValueAsString(MutableMap()) should be("{}")
	}
}