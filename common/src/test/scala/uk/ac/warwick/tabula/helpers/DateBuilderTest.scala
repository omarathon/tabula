package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.web.views.FreemarkerRendering

// scalastyle:off magic.number
class DateBuilderTest extends TestBase with FreemarkerRendering {

  import DateBuilder._

  @Test def formatting {
    withFakeTime(new DateTime(2012, 4, 12, 13, 36, 0)) {

      val anotherDay = new DateTime(2012, 3, 10, 12, 13, 14)
        .withZoneRetainFields(DateTimeZone.forID("Europe/London"))

      val yesterday = new DateTime().minusDays(1)
      val tomorrow = new DateTime().plusDays(1)
      val today = new DateTime()

      // Test default settings
      format(anotherDay) should be("12:13&#8194;Sat 10ᵗʰ March 2012")

      format(anotherDay, false, false, false, true, true, false, false, true, false) should be("12:13&#8194;Sat 10<sup>th</sup> March 2012")
      format(anotherDay, true, true, true, true, true, false, false, true, false) should be("12:13:14 (GMT)&#8194;Sat 10<sup>th</sup> March 2012")

      format(yesterday, false, true, false, true, true, false, false, true, false) should be("13:36&#8194;Yesterday")
      format(tomorrow, true, true, false, false, true, false, false, true, false) should be("13:36:00&#8194;tomorrow")
      format(today, false, false, false, true, true, false, false, true, false) should be("13:36&#8194;Today")

      format(today, false, false, false, true, false, false, false, true, false) should be("13:36&#8194;Thu 12<sup>th</sup> April 2012")
      format(today, false, false, false, true, false, false, true, true, true) should be("13:36&#8194;Thu 12<sup>th</sup> Apr")

      // Freemarker exec
      implicit val fmConfig = newFreemarkerConfiguration
      val rendered = renderToString("dateBuilder.ftl", Map("b" -> new DateBuilder, "today" -> today))
      rendered should be("13:36&#8194;Today")
    }
  }

  @Test def ordinals {
    for ((i, o) <- Seq(
      0 -> "ᵗʰ",
      1 -> "ˢᵗ",
      2 -> "ⁿᵈ",
      3 -> "ʳᵈ",
      4 -> "ᵗʰ",
      5 -> "ᵗʰ",
      10 -> "ᵗʰ",
      11 -> "ᵗʰ",
      12 -> "ᵗʰ",
      13 -> "ᵗʰ",
      20 -> "ᵗʰ",
      21 -> "ᵗʰ",
      101 -> "ᵗʰ"
    )) withClue("Ordinal of " + i) {
      ordinal(i) should be(o)
    }
  }
}