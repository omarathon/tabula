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

      format(date = anotherDay, includeAt = false) should be("12:13&#8194;Sat 10ᵗʰ March 2012")

      format(date = anotherDay, includeSeconds = true, includeTimezone = true) should be("12:13:14 (GMT)&#8194;Sat 10ᵗʰ March 2012")

      format(date = yesterday) should be("13:36&#8194;Yesterday")
      format(date = tomorrow, includeSeconds = true, capitalise = false) should be("13:36:00&#8194;tomorrow")
      format(date = today, includeAt = false) should be("13:36&#8194;Today")

      format(date = today, includeAt = false, relative = false) should be("13:36&#8194;Thu 12ᵗʰ April 2012")

      format(date = today, includeAt = false, relative = false, shortMonth = true, excludeCurrentYear = true) should be("13:36&#8194;Thu 12ᵗʰ Apr")

      // Freemarker exec
      implicit val fmConfig = newFreemarkerConfiguration
      val rendered = renderToString("dateBuilder.ftl", Map("b" -> new DateBuilder, "today" -> today))
      rendered should be("13:36&#8194;Today")
    }
  }

  @Test def ordinals() {
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
      21 -> "ˢᵗ",
      101 -> "ˢᵗ"
    )) withClue("Ordinal of " + i) {
      ordinal(i) should be(o)
    }
  }
}