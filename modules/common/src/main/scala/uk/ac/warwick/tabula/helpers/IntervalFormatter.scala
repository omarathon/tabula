package uk.ac.warwick.tabula.helpers

import org.joda.time._
import org.joda.time.format.DateTimeFormat
import collection.JavaConversions._
import freemarker.template.TemplateMethodModelEx
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateModel
import uk.ac.warwick.tabula.JavaImports._

/**
	Formats an Interval (which is a start and an end date together)
	in a compact format that avoids repetition. If start and end are
	in the same year, for example, the year is only printed at the end.
*/
object IntervalFormatter {

	private val hourMinuteFormat = DateTimeFormat.forPattern("HH:mm")
	private val dayAndDateFormat = DateTimeFormat.forPattern("EE d")
	private val dateFormatWithoutDay = DateTimeFormat.forPattern("d")
	private val monthFormat = DateTimeFormat.forPattern(" MMM")
	private val monthAndYearFormat = DateTimeFormat.forPattern(" MMM yyyy")

	/** Print date range in this format:
	  *
	  *     09:00 Wed 10th Oct - 12:00 Mon 5th Nov 2012
	  *
	  * or this format if the years differ:
	  *
	  *     09:00 Wed 10th Oct 2012 - 12:00 Mon 5th Nov 2013
	  *
	  * Seconds are never printed.
		*
		* Includes time by default, but set includeTime=false to not include time.
	  */
	def format(start: DateTime, end: DateTime, includeTime: Boolean = true, includeDays:Boolean = true) = {
		val yearAtStart = (start.getYear != end.getYear)
		doFormat(start, yearAtStart, includeTime, includeDays) + " - " + doFormat(end, true, includeTime, includeDays)
	}


	/** Useful sometimes if you have an "endless" interval like an open-ended Assignment. */
	def format(start: DateTime): String = doFormat(start, true)

	def format(start: DateTime, includeTime: Boolean): String = doFormat(start, true, includeTime)

	/** @see #format(DateTime, DateTime, Boolean) */
	def format(interval: Interval): String = format(interval.getStart, interval.getEnd)

	private def doFormat(date: DateTime, includeYear: Boolean, includeTime: Boolean = true, includeDays:Boolean = true) = {
		
		// TAB-546 : This was previously in a 12-hour format, e.g. 9am, 9:15am, 12 noon, 12 midnight
		// now 24 hour format
		def timePart(date: DateTime) = {
			hourMinuteFormat.print(date).toLowerCase
		}

		// e.g. Mon 5th Nov
		def dayPart(date: DateTime, includeDays:Boolean) = {
			(if (includeDays) dayAndDateFormat else dateFormatWithoutDay)
			    .print(date) + "<sup>" + DateBuilder.ordinal(date.getDayOfMonth) + "</sup>"
		}

		// e.g. Jan 2012, Nov 2012, Mar, Apr
		def monthYearPart(date: DateTime, includeYear: Boolean) = {
			if (includeYear) monthAndYearFormat.print(date)
			else monthFormat.print(date)
		}

		val datePart = dayPart(date, includeDays) + monthYearPart(date, includeYear)

		if (includeTime) timePart(date) + " " + datePart
		else datePart
	}
}


/**
  * Companion class for Freemarker.
  */
class IntervalFormatter extends TemplateMethodModelEx {
	import IntervalFormatter.format

	/** Two-argument method taking a start and end date. */
	override def exec(list: JList[_]) = {
		val args = list.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }
		args match {
			case Seq(start: DateTime) => format(start)
			case Seq(start: DateTime, end: DateTime) => format(start, end)
			case _ => throw new IllegalArgumentException("Bad args")
		}
	}
}
