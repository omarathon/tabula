package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.data.model.groups.{WeekRange}
import org.joda.time._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.{WeekToDateConverterComponent}

case class EventOccurrence(
														name: String,
														description: String,
														eventType: TimetableEventType,
														start: LocalDateTime,
														end: LocalDateTime,
														location: Option[String],
														moduleCode: String,
														staffUniversityIds: Seq[String])

object EventOccurrence {
	def apply(timetableEvent: TimetableEvent, start: LocalDateTime, end: LocalDateTime): EventOccurrence = {
		new EventOccurrence(
			timetableEvent.name,
			timetableEvent.description,
			timetableEvent.eventType,
			start,
			end,
			timetableEvent.location,
			timetableEvent.moduleCode,
			timetableEvent.staffUniversityIds
		)
	}
}
trait EventOccurrenceServiceComponent{
	val eventOccurrenceService:EventOccurrenceService
	trait EventOccurrenceService{
		def fromTimetableEvent(event: TimetableEvent, dateRange: Interval): Seq[EventOccurrence]
	}
}
trait TermBasedEventOccurrenceComponent extends EventOccurrenceServiceComponent{
	this: WeekToDateConverterComponent =>
	val eventOccurrenceService: EventOccurrenceService = new TermBasedEventOccurrenceService

	class TermBasedEventOccurrenceService extends EventOccurrenceService{

		def fromTimetableEvent(event: TimetableEvent, dateRange: Interval): Seq[EventOccurrence] = {

			// lord preserve us from trying to generate a timetable that spans two academic years.
			val year = AcademicYear.guessByDate(dateRange.getStart)
			def eventDateToLocalDate(week: WeekRange.Week, localTime: LocalTime): LocalDateTime = {
				weekToDateConverter.toLocalDatetime(week, event.day, localTime, year).
					// Considered just returning None here, but if we ever encounter an event who's week/day/time
					// specifications can't be converted into calendar dates, we have big problems with
					// data quality and we need to fix them.
					getOrElse(throw new RuntimeException("Unable to obtain a date for " + event))
			}

			val weeks = for {
				weekRange <- event.weekRanges
				week <- weekRange.toWeeks
			} yield week

			val eventsInIntersectingWeeks = weeks.
				filter(week => weekToDateConverter.intersectsWeek(dateRange, week, year)).
				map {
				week =>
					EventOccurrence(event,
						eventDateToLocalDate(week, event.startTime),
						eventDateToLocalDate(week, event.endTime)
					)
			}

			// do not remove; import needed for sorting
			import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
			eventsInIntersectingWeeks
				.filterNot(_.end.toDateTime().isBefore(dateRange.getStart))
				.filterNot(_.start.toDateTime().isAfter(dateRange.getEnd))
				.sortBy(_.start)
		}
	}

}
