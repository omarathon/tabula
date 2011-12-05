package uk.ac.warwick.courses.helpers
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import org.joda.time.Duration
import org.joda.time.Duration._

class DurationFormatterTest extends TestBase {
	@Test def format {
		val duration = standardDays(5).plus(standardHours(4)).plus(standardMinutes(1))
		DurationFormatter.format(duration) should be ("5 days 4 hours 1 minute")
	}
}