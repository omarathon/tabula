package uk.ac.warwick.tabula.web.views
import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import freemarker.cache.ClassTemplateLoader
import java.io.StringWriter
import collection.JavaConversions._
import org.junit.Before
import freemarker.template.Configuration
import uk.ac.warwick.tabula.data.model.Department
import org.junit.Ignore
import org.joda.time.Duration

class FreemarkerEngineTest extends TestBase {
	var configuration:Configuration = _
	
	@Before def setup {
		configuration = newFreemarkerConfiguration
	}
	
	def render(template:String, map:Map[String,Any]) = {
		val writer = new StringWriter
		configuration.getTemplate(template).process(map, writer)
		writer.toString
	}
	
	@Test def plain {
		val num:Option[Duration] = Some(Duration.standardSeconds(1))
		val output = render("plain.ftl", Map(
				"specifiedValue" -> "Specified value.",
				"longnum" -> num
		))
		output should be ("No frills template #1. Unspecified value. Specified value.")
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

  @Test def usesMacros{
    val output=render("uses_macros.ftl",Map("count"->3))
    output should be ("First line\n3 wombats")
  }
	
}