package uk.ac.warwick.tabula.profiles.web.views

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.profiles.services.timetables.EventOccurrence
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

/**
 * serialises to the JSON which FullCalendar likes.
 *
 * Note: start and end are *Seconds* since the epoch, not milliseconds!
 */
case class FullCalendarEvent(title:String,
														 allDay:Boolean,
														 start:Long,
														 end:Long,
														 // fields below here are not used by FullCalendar itself, they're custom fields
                             // for use in the renderEvent callback
														 formattedStartTime:String,
														 formattedEndTime:String,
														 location:String="",
														 description:String="" )

object FullCalendarEvent{
	def apply(source:EventOccurrence):FullCalendarEvent = {
		val shortTimeFormat = DateTimeFormat.shortTime()
		FullCalendarEvent(
			source.moduleCode.toUpperCase + " " +  source.eventType.displayName + source.location.map(l=>s" ($l)").getOrElse(""),
			false,
			source.start.toDateTime.getMillis/1000,
			source.end.toDateTime.getMillis/1000,
		  shortTimeFormat.print(source.start.toDateTime),
			shortTimeFormat.print(source.end.toDateTime),
		  source.location.getOrElse(""),
		  source.description
		)
	}
}