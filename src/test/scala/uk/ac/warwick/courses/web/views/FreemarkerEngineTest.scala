package uk.ac.warwick.courses.web.views
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import freemarker.cache.ClassTemplateLoader
import java.io.StringWriter
import collection.JavaConversions._
import org.junit.Before
import freemarker.template.Configuration

class FreemarkerEngineTest extends TestBase {
	var configuration:Configuration = _
	def newConfiguration = new ScalaFreemarkerConfiguration {
		setTemplateLoader(new ClassTemplateLoader(getClass, "/freemarker/"))
		setAutoIncludes(Nil) // don't use prelude
	}
	@Before def setup {
		configuration = newConfiguration
	}
	
	def render(template:String, map:Map[String,Any]) = {
		val writer = new StringWriter
		configuration.getTemplate(template).process(map, writer)
		writer.toString
	}
	
	@Test def plain {
		val output = render("plain.ftl", Map("specifiedValue" -> "Specified value."))
		output should be ("No frills template. Unspecified value. Specified value.")
	}
	
	@Test def someAndNon {
		val output = render("somenone.ftl", Map(
			"some" -> Some("CERBERUS"),
			"none" -> None
		))
		output should be ("Robot=CERBERUS;")
	}
	
}