package uk.ac.warwick.tabula.helpers

import org.joda.time.{LocalDateTime, LocalTime}
import uk.ac.warwick.tabula.TestBase

class DateRangeTest extends TestBase {

  // some months
  private val april = DateRange.parse("2019-04-01", "2019-04-30")
  private val may = DateRange.parse("2019-05-01", "2019-05-31")

  // some dates and times that may fall within a month
  private val midday = LocalTime.parse("12:00:00")
  private val endOfMarch = april.lowerEndpoint().minusDays(1).toLocalDateTime(midday)
  private val firstApril = april.lowerEndpoint().toLocalDateTime(midday)
  private val veryStartOfApril = april.lowerEndpoint().toLocalDateTime(LocalTime.MIDNIGHT)
  private val endOfApril = april.upperEndpoint().toLocalDateTime(midday)
  private val firstOfMay = april.upperEndpoint().plusDays(1).toLocalDateTime(midday)

  @Test
  def monthsSingleMonth(): Unit = {
    val ranges = DateRange.months(DateRange.parse("2019-04-03", "2019-04-23"))
    ranges shouldBe Seq(april)
  }

  @Test
  def monthsTwoMonths(): Unit = {
    val ranges = DateRange.months(DateRange.parse("2019-04-03", "2019-05-23"))
    ranges shouldBe Seq(april, may)
  }

  @Test
  def monthsOneExtraDay(): Unit = {
    // Include all of May even if it's just to get the first day of May.
    val ranges = DateRange.months(DateRange.parse("2019-04-03", "2019-05-01"))
    ranges shouldBe Seq(april, may)
  }

  @Test
  def inRangeObviouslyNot(): Unit = {
    assertNotInApril(endOfMarch)
    assertNotInApril(firstOfMay)
  }

  @Test
  def inRange(): Unit = {
    assertInApril(firstApril)
    assertInApril(veryStartOfApril)
    assertInApril(endOfApril)
  }

  private def assertInApril(dateTime: LocalDateTime): Unit = {
    withClue(s"$dateTime is in April") {
      DateRange.inRange(april, dateTime) shouldBe true
    }
  }

  private def assertNotInApril(dateTime: LocalDateTime): Unit = {
    withClue(s"$dateTime is not in April") {
      DateRange.inRange(april, dateTime) shouldBe false
    }
  }

}
