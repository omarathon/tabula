package uk.ac.warwick.tabula.helpers

import freemarker.template.{TemplateMethodModelEx, TemplateModel}
import freemarker.template.utility.DeepUnwrap
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConverters._

object DateBuilder {

  private val dayAndDateFormat = DateTimeFormat.forPattern("EEE d")
  private val monthFormat = DateTimeFormat.forPattern(" MMMM")
  private val shortMonthFormat = DateTimeFormat.forPattern(" MMM")
  private val monthAndYearFormat = DateTimeFormat.forPattern(" MMMM yyyy")
  private val shortMonthAndYearFormat = DateTimeFormat.forPattern(" MMM yyyy")
  private val formatterMap = new DateTimeFormatterCache

  private val _relativeWords = Map(
    'yesterday -> "Yesterday",
    'today -> "Today",
    'tomorrow -> "Tomorrow")
  private val relativeWords = Map(
    true -> _relativeWords,
    false -> _relativeWords.mapValues(_.toLowerCase))

  /* everything is specified, including whether minutes should be included */
  def format(date: ReadableDateTime,
    includeSeconds: Boolean = false,
    includeAt: Boolean = true,
    includeTimezone: Boolean = false,
    capitalise: Boolean = true,
    relative: Boolean = true,
    split: Boolean = false,
    shortMonth: Boolean = false,
    includeTime: Boolean = true,
    excludeCurrentYear: Boolean = false,
  ): String = {
    if (includeTime) {
      val pattern = new StringBuilder
      pattern.append("HH:mm")
      if (includeSeconds) pattern.append(":ss")
      if (includeTimezone) pattern.append(" (z)")
      //if (includeAt) pattern.append(" 'at'")
      (formatterMap.retrieve(pattern.toString()) print date).trim() + (if (split) "<br />" else "&#8194;") + datePart(date, capitalise, relative, shortMonth, excludeCurrentYear)
    } else {
      datePart(date, capitalise, relative, shortMonth, excludeCurrentYear)
    }
  }

  def ordinal(day: Int): String = day % 10 match {
    case _ if (day >= 10 && day <= 20) => "ᵗʰ"
    case 1 => "ˢᵗ"
    case 2 => "ⁿᵈ"
    case 3 => "ʳᵈ"
    case _ => "ᵗʰ"
  }

  def datePart(date: ReadableDateTime, capitalise: Boolean, relative: Boolean, shortMonth: Boolean, excludeCurrentYear: Boolean): String = {
    val today = LocalDate.now.toDateTimeAtStartOfDay
    val thatDay = new LocalDate(date.getMillis, date.getChronology).toDateTimeAtStartOfDay

    lazy val absoluteDate = (dayAndDateFormat print date) + ordinal(date.getDayOfMonth) +
      (if (excludeCurrentYear && today.getYear == thatDay.getYear) {
        (if (shortMonth) (shortMonthFormat print date)
        else (monthFormat print date))
      } else {
        (if (shortMonth) (shortMonthAndYearFormat print date)
        else (monthAndYearFormat print date))
      })

    if (!relative) absoluteDate
    else if (today isEqual thatDay) relativeWords(capitalise)('today)
    else if (today.minusDays(1) isEqual thatDay) relativeWords(capitalise)('yesterday)
    else if (today.plusDays(1) isEqual thatDay) relativeWords(capitalise)('tomorrow)
    else absoluteDate
  }
}

class DateBuilder extends TemplateMethodModelEx {

  import DateBuilder.format

  /** For Freemarker */
  override def exec(list: JList[_]): String = {
    val args = list.asScala.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }

    val date = args.head match {
      case partial: LocalDate => partial.toDateTimeAtStartOfDay
      case local: LocalDateTime => local.toDateTime
      case instant: ReadableDateTime => instant
      case _ => throw new IllegalArgumentException("Bad date argument")
    }

    args.tail match {
      case Seq(secs: JBoolean, at: JBoolean, tz: JBoolean, caps: JBoolean, relative: JBoolean, split: JBoolean, shortMonth: JBoolean, includeTime: JBoolean, excludeCurrentYear: JBoolean) =>
        format(date = date,
          includeSeconds = secs,
          includeAt = at,
          includeTimezone = tz,
          capitalise = caps,
          relative = relative,
          split = split,
          shortMonth = shortMonth,
          includeTime = includeTime,
          excludeCurrentYear = excludeCurrentYear,
        )
      case _ => throw new IllegalArgumentException("Bad args")
    }
  }
}
