package uk.ac.warwick.courses.web.views
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import freemarker.cache.ClassTemplateLoader
import java.io.StringWriter
import collection.JavaConversions._
import org.junit.Before
import freemarker.template.Configuration
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.web.Routes
import org.junit.Ignore

class FreemarkerEngineTest extends TestBase {
	var configuration:Configuration = _
	def newConfiguration = new ScalaFreemarkerConfiguration {
		setTemplateLoader(new ClassTemplateLoader(getClass, "/freemarker/"))
		setSharedVariables(Map("Routes" -> Routes))
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
	
	/**
	 * The Scala beans wrapper doesn't handle method invocations yet
	 */
	@Ignore @Test def routes {
		val department = new Department
		department.code = "la"
		val output = render("renderroutes.ftl", Map("department" -> department))
		output should be ("GURP")
	}
	
	@Test def someAndNon {
		val output = render("somenone.ftl", Map(
			"some" -> Some("CERBERUS"),
			"none" -> None
		))
		output should be ("Robot=CERBERUS;")
	}
	
}