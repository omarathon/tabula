package uk.ac.warwick.tabula.profiles.web.views

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.data.model.MapLocation
import uk.ac.warwick.tabula.helpers.ConfigurableIntervalFormatter
import uk.ac.warwick.tabula.helpers.ConfigurableIntervalFormatter.{Hour12OptionalMins, IncludeDays}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

/**
 * serialises to the JSON which FullCalendar likes.
 *
 * Note: start and end are *Seconds* since the epoch, not milliseconds!
 */
case class FullCalendarEvent(
	title: String,
	fullTitle: String,
	allDay: Boolean,
	start: Long,
	end: Long,
	backgroundColor: String="#4daacc", // tabulaBlueLight.
	borderColor: String="#4daacc",
	textColor: String="#000",
	// fields below here are not used by FullCalendar itself, they're custom fields
	// for use in the renderEvent callback
	formattedStartTime: String,
	formattedEndTime: String,
	formattedInterval: String,
	location: String = "",
	locationId: String = "", // for map links
	description: String = "",
	shorterTitle: String = "", // used in the pop-up to display event details
	tutorNames: String = "",
	parentType: String = "Empty",
	parentShortName: String = "",
	parentFullName: String = "",
	comments: String = ""
)

object FullCalendarEvent {

	def apply(source: EventOccurrence, userLookup: UserLookupService): FullCalendarEvent = {
		val intervalFormatter = new ConfigurableIntervalFormatter(Hour12OptionalMins, IncludeDays)

		val shortTimeFormat = DateTimeFormat.shortTime()

		// Some event providers (namely Celcat) have events scheduled from xx:05 to xx:55.
		// This is fine for displaying the formatted time, but for the seconds time it's neater
		// to roll these times to the hour
		def rollToHour(dt: DateTime) =
			if (dt.getMinuteOfHour == 5) dt.minusMinutes(5)
			else if (dt.getMinuteOfHour == 55) dt.plusMinutes(5)
			else dt

		val startTimeSeconds = rollToHour(source.start.toDateTime).getMillis / 1000
		val endTimeSeconds = rollToHour(source.end.toDateTime).getMillis / 1000

		val parentTitle = source.parent match {
			case m: TimetableEvent.Module => for (sn <- m.shortName; fn <- m.fullName) yield s"$sn $fn"
			case o: TimetableEvent.Parent => o.shortName
		}

		val title =
			Seq(parentTitle, Some(source.eventType.displayName), source.location.map { l => s"(${l.name})" })
				.flatten
				.mkString(" ")

		FullCalendarEvent(
			title = title,
			fullTitle = source.title,
			allDay = false,
			start = startTimeSeconds,
			end = endTimeSeconds,
			formattedStartTime = shortTimeFormat.print(source.start.toDateTime),
			formattedEndTime = shortTimeFormat.print(source.end.toDateTime),
			formattedInterval = intervalFormatter.format(source.start.toDateTime, source.end.toDateTime),
			location = source.location.fold("") { _.name },
			locationId = source.location.collect { case l: MapLocation => l }.fold("") { _.locationId },
			description = source.description,
			shorterTitle = source.parent.shortName.map { _ + " " }.getOrElse("") + source.eventType.displayName,
			tutorNames = userLookup.getUsersByWarwickUniIds(source.staffUniversityIds).values.map(_.getFullName).mkString(", "),
		  parentType = source.parent match {
				case TimetableEvent.Empty(_,_) => "Empty"
				case TimetableEvent.Module(_,_) => "Module"
				case TimetableEvent.Relationship(_,_) => "Relationship"
			},
			parentShortName = source.parent.shortName.getOrElse(""),
			parentFullName = source.parent.fullName.getOrElse(""),
			comments = source.comments.getOrElse("")
		)
	}
}

