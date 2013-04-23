package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import collection.JavaConversions._
import uk.ac.warwick.tabula.web.views.FreemarkerRendering

// scalastyle:off magic.number
class DateBuilderTest extends TestBase with FreemarkerRendering {
	
	import DateBuilder._

	@Test def formatting {
		withFakeTime(new DateTime(2012,4,12, 13,36,0)) {
			
			val anotherDay = new DateTime(2012,3,10, 12,13,14)
			
			val yesterday = new DateTime().minusDays(1)
			val tomorrow = new DateTime().plusDays(1)
			val today = new DateTime()
			
			// Test default settings
			format(anotherDay) should be ("Sat 10th March 2012&#8194;at 12:13")
			
			format(anotherDay, false, false, false, true, true, false, false, true) should be ("Sat 10th March 2012&#8194;12:13")
			format(anotherDay, true, true, true, true, true, false, false, true) should be ("Sat 10th March 2012&#8194;at 12:13:14 (GMT)")
			
			format(yesterday, false, true, false, true, true, false, false, true) should be ("Yesterday&#8194;at 13:36")
			format(tomorrow, true, true, false, false, true, false, false, true) should be ("tomorrow&#8194;at 13:36:00")
			format(today, false, false, false, true, true, false, false, true) should be ("Today&#8194;13:36")
			
			format(today, false, false, false, true, false, false, false, true) should be ("Thu 12th April 2012&#8194;13:36")
		  
			// Freemarker exec
			implicit val fmConfig = newFreemarkerConfiguration
			val rendered = renderToString("dateBuilder.ftl", Map("b" -> new DateBuilder, "today" -> today))
			rendered should be ("Today&#8194;13:36")
		}
	}
	
	@Test def ordinals {
		for ((i, o) <- Seq(
				0->"th",
				1->"st",
				2->"nd",
				3->"rd",
				4->"th",
				5->"th",
				10->"th",
				11->"th",
				12->"th",
				13->"th",
				20->"th",
				21->"st",
				101->"st"
		)) withClue("Ordinal of "+i) { ordinal(i) should be (o) } 
	}
}