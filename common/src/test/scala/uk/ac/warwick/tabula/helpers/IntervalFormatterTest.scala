package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.joda.time.{DateTime, Interval}
import freemarker.template.{ObjectWrapper, TemplateModel, SimpleHash}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.ConfigurableIntervalFormatter.{IncludeDays, Hour12OptionalMins}

// scalastyle:off magic.number
class IntervalFormatterTest extends TestBase {

  import IntervalFormatter.format

  // TAB-546 hourminute format changed

  @Test
  def sameYear: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 0, 0)
    val close = new DateTime(2012, 11, 5, /**/ 12, 0, 0)

    format(open, close) should be("09:00, Wed 10ᵗʰ Oct - 12:00, Mon 5ᵗʰ Nov 2012")
    format(new Interval(open, close)) should be("09:00, Wed 10ᵗʰ Oct - 12:00, Mon 5ᵗʰ Nov 2012")
  }

  @Test
  def sameYearExcludeTimes: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 0, 0)
    val close = new DateTime(2012, 11, 5, /**/ 12, 0, 0)
    format(open, close, includeTime = false) should be("Wed 10ᵗʰ Oct - Mon 5ᵗʰ Nov 2012")
  }

  @Test
  def onTheHourOmitMinutes: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 0, 0)
    val close = new DateTime(2012, 11, 5, /**/ 12, 0, 0)
    val formatter = new ConfigurableIntervalFormatter(Hour12OptionalMins, IncludeDays)
    formatter.format(new Interval(open, close)) should be("9am, Wed 10ᵗʰ Oct - 12pm, Mon 5ᵗʰ Nov 2012")
  }

  @Test
  def endOffTheHourOmitMinutes: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 0, 0)
    val close = new DateTime(2012, 11, 5, /**/ 12, 15, 0)
    val formatter = new ConfigurableIntervalFormatter(Hour12OptionalMins, IncludeDays)
    formatter.format(new Interval(open, close)) should be("9am, Wed 10ᵗʰ Oct - 12:15pm, Mon 5ᵗʰ Nov 2012")
  }

  @Test
  def startOffTheHourOmitMinutes: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 15, 0)
    val close = new DateTime(2012, 11, 5, /**/ 12, 0, 0)
    val formatter = new ConfigurableIntervalFormatter(Hour12OptionalMins, IncludeDays)

    formatter.format(new Interval(open, close)) should be("9:15am, Wed 10ᵗʰ Oct - 12pm, Mon 5ᵗʰ Nov 2012")
  }


  @Test
  def sameYearExcludeDays: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 0, 0)
    val close = new DateTime(2012, 11, 5, /**/ 12, 0, 0)
    format(open, close, includeDays = false) should be("09:00, 10ᵗʰ Oct - 12:00, 5ᵗʰ Nov 2012")
  }

  /* When year changes, specify year both times. */
  @Test
  def differentYear: Unit = {
    val open = new DateTime(2012, 12, 10, /**/ 9, 0, 0)
    val close = new DateTime(2013, 1, 15, /**/ 12, 0, 0)
    format(open, close) should be("09:00, Mon 10ᵗʰ Dec 2012 - 12:00, Tue 15ᵗʰ Jan 2013")
  }

  @Test
  def partPastTheHour: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 15, 7)
    val close = new DateTime(2012, 11, 5, /**/ 14, 0, 7)
    format(open, close) should be("09:15, Wed 10ᵗʰ Oct - 14:00, Mon 5ᵗʰ Nov 2012")
  }

  @Test
  def sameDate: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 15, 7)
    val close = new DateTime(2012, 10, 10, /**/ 14, 0, 7)
    format(open, close) should be("09:15 - 14:00, Wed 10ᵗʰ Oct 2012")
  }

  @Test
  def endless: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 15, 7)
    format(open) should be("09:15, Wed 10ᵗʰ Oct 2012")
  }

  @Test
  def endlessExcludeTime: Unit = {
    val open = new DateTime(2012, 10, 10, /**/ 9, 15, 7)
    format(open, includeTime = false) should be("Wed 10ᵗʰ Oct 2012")
  }

  @Test def freemarker: Unit = {
    val formatter = new IntervalFormatter

    val args: JList[TemplateModel] = JArrayList()

    // Use a SimpleHash as a workaround to wrapping things manually
    val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
    model.put("start", new DateTime(2012, 10, 10, /**/ 9, 15, 7))
    model.put("end", new DateTime(2012, 11, 5, /**/ 0, 0, 7))

    args.add(model.get("start"))

    formatter.exec(args) should be("09:15, Wed 10ᵗʰ Oct 2012")

    args.add(model.get("end"))

    formatter.exec(args) should be("09:15, Wed 10ᵗʰ Oct - 00:00, Mon 5ᵗʰ Nov 2012")
  }

}