package uk.ac.warwick.tabula.coursework.helpers

import uk.ac.warwick.tabula.coursework.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import collection.JavaConversions._
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.coursework.web.views.FreemarkerRendering


class DateBuilderFreemarkerTest extends TestBase with FreemarkerRendering {
	
	@Test def format {
		withFakeTime(new DateTime(2012,4,12, 13,36,0)) {
			val builder = new DateBuilder
			val today = new DateTime()
		  
			// Freemarker exec
			implicit val fmConfig = newFreemarkerConfiguration
			val rendered = renderToString("dateBuilder.ftl", Map("b" -> builder, "today" -> today))
			rendered should be ("Today 13:36")
		}
	}
}