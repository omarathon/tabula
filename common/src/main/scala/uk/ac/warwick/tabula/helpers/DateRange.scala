package uk.ac.warwick.tabula.helpers

import com.google.common.collect.{Range => GRange}
import org.joda.time.{LocalDate, LocalDateTime}
import uk.ac.warwick.tabula.timetables.EventOccurrence

import scala.collection.mutable.ArrayBuffer

object DateRange {
  def apply(firstDay: LocalDate, lastDay: LocalDate): GRange[LocalDate] =
    GRange.closed(firstDay, lastDay)

  def parse(start: String, end: String): GRange[LocalDate] =
    DateRange(LocalDate.parse(start), LocalDate.parse(end))

  def inRange(range: GRange[LocalDate], dateTime: LocalDateTime): Boolean =
    range.contains(dateTime.toLocalDate) // ignore time, our ranges cover the whole day

  /** Whether this event appears in this date range. */
  def inRange(range: GRange[LocalDate])(event: EventOccurrence): Boolean =
    inRange(range, event.start) || inRange(range, event.end)

  /**
   * Given a single range of dates, returns a sequence of ranges representing the
   * whole months that contain this range.
   *
   * start and end are the first and last days of each month (since our ranges are inclusive).
   */
  def months(range: GRange[LocalDate]): Seq[GRange[LocalDate]] = {
    val start = range.lowerEndpoint()
    val end = range.upperEndpoint()

    var monthStart = start.withDayOfMonth(1)
    val datas = ArrayBuffer[GRange[LocalDate]]()
    do {
      datas += DateRange(monthStart, monthStart.plusMonths(1).minusDays(1))
      monthStart = monthStart.plusMonths(1)
    } while (monthStart.equals(end) || monthStart.isBefore(end))
    datas.toSeq
  }
}
