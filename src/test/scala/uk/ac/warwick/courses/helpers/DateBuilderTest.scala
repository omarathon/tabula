package uk.ac.warwick.courses.helpers

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import collection.JavaConversions._

class DateBuilderTest extends TestBase with FreemarkerRendering {
	
	@Test def format {
		withFakeTime(new DateTime(2012,4,12, 13,36,0)) {
		 
			val builder = new DateBuilder
			
			val anotherDay = new DateTime(2012,3,10, 12,13,14)
			
			val yesterday = new DateTime().minusDays(1)
			val tomorrow = new DateTime().plusDays(1)
			val today = new DateTime()
			
			builder.format(anotherDay, false, false, false, true, true) should be ("Sat 10th March 2012 12:13")
			builder.format(anotherDay, true, true, true, true, true) should be ("Sat 10th March 2012 at 12:13:14 (GMT)")
			
			builder.format(yesterday, false, true, false, true, true) should be ("Yesterday at 13:36")
			builder.format(tomorrow, true, true, false, false, true) should be ("tomorrow at 13:36:00")
			builder.format(today, false, false, false, true, true) should be ("Today 13:36")
			
			builder.format(today, false, false, false, true, false) should be ("Thu 12th April 2012 13:36")
			
			// Freemarker exec
			implicit val fmConfig = newFreemarkerConfiguration
			val rendered = renderToString("dateBuilder.ftl", Map("b" -> builder, "today" -> today))
			rendered should be ("Today 13:36")
		}
	}
	
	@Test def ordinals {
		val builder = new DateBuilder
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
		)) withClue("Ordinal of "+i) { builder.ordinal(i) should be (o) } 
	}
}