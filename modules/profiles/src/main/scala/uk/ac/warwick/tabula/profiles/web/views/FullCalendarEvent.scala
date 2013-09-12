package uk.ac.warwick.tabula.profiles.web.views

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.profiles.services.timetables.EventOccurrence
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.helpers.{ConfigurableIntervalFormatter, IntervalFormatter}
import uk.ac.warwick.tabula.helpers.ConfigurableIntervalFormatter.{IncludeDays, Hour12OptionalMins}

/**
 * serialises to the JSON which FullCalendar likes.
 *
 * Note: start and end are *Seconds* since the epoch, not milliseconds!
 */
case class FullCalendarEvent(title: String,
														 allDay: Boolean,
														 start: Long,
														 end: Long,
														 backgroundColor:String="#4daacc", // tabulaBlueLight.
														 borderColor:String="#4daacc",
														 textColor:String="#000",
														 // fields below here are not used by FullCalendar itself, they're custom fields
														 // for use in the renderEvent callback
														 formattedStartTime: String,
														 formattedEndTime: String,
														 formattedInterval: String,
														 location: String = "",
														 description: String = "",
														 shorterTitle: String = "", // used in the pop-up to display event details
														 tutorNames: String = "",
                             moduleCode: String="")

object FullCalendarEvent {

	def apply(source: EventOccurrence, userLookup: UserLookupService): FullCalendarEvent = {
		val intervalFormatter = new ConfigurableIntervalFormatter(Hour12OptionalMins, IncludeDays)
		val shortTimeFormat = DateTimeFormat.shortTime()
		FullCalendarEvent(
			title = source.moduleCode.toUpperCase + " " + source.eventType.displayName + source.location.map(l => s" ($l)").getOrElse(""),
			allDay = false,
			start = source.start.toDateTime.getMillis / 1000,
			end = source.end.toDateTime.getMillis / 1000,
			formattedStartTime = shortTimeFormat.print(source.start.toDateTime),
			formattedEndTime = shortTimeFormat.print(source.end.toDateTime),
			formattedInterval = intervalFormatter.format(source.start.toDateTime, source.end.toDateTime),
			location = source.location.getOrElse(""),
			description = source.description,
			shorterTitle = source.moduleCode.toUpperCase + " " + source.eventType.displayName,
			tutorNames = source.staffUniversityIds.map(id => userLookup.getUserByWarwickUniId(id).getFullName).mkString(", "),
		  moduleCode = source.moduleCode
		)
	}
}

