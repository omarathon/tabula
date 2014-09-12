package uk.ac.warwick.tabula.services

import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter
import net.fortuna.ical4j.model.parameter.{Cn, Value}
import net.fortuna.ical4j.model.property._
import org.apache.commons.codec.digest.DigestUtils
import org.joda.time._
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}


trait EventOccurrenceServiceComponent{
	val eventOccurrenceService: EventOccurrenceService
}

trait TermBasedEventOccurrenceComponent extends EventOccurrenceServiceComponent {
	val eventOccurrenceService: TermBasedEventOccurrenceService
}

trait AutowiringTermBasedEventOccurrenceServiceComponent extends TermBasedEventOccurrenceComponent {
	val eventOccurrenceService: TermBasedEventOccurrenceService = Wire[TermBasedEventOccurrenceService]
}

trait EventOccurrenceService {
	def fromTimetableEvent(event: TimetableEvent, dateRange: Interval): Seq[EventOccurrence]
	def toVEvent(eventOccurrence: EventOccurrence): VEvent
}

abstract class TermBasedEventOccurrenceService extends EventOccurrenceService {

	self: WeekToDateConverterComponent with TermServiceComponent with ProfileServiceComponent =>

	def fromTimetableEvent(event: TimetableEvent, dateRange: Interval): Seq[EventOccurrence] = {

		def eventDateToLocalDate(week: WeekRange.Week, localTime: LocalTime): LocalDateTime = {
			weekToDateConverter.toLocalDatetime(week, event.day, localTime, event.year).
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
			filter(week => weekToDateConverter.intersectsWeek(dateRange, week, event.year)).
			map {
			week =>
				EventOccurrence(event,
					eventDateToLocalDate(week, event.startTime),
					eventDateToLocalDate(week, event.endTime)
				)
		}

		// do not remove; import needed for sorting
		// should be: import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		eventsInIntersectingWeeks
			.filterNot(_.end.toDateTime.isBefore(dateRange.getStart))
			.filterNot(_.start.toDateTime.isAfter(dateRange.getEnd))
			.sortBy(_.start)
	}

	def toVEvent(eventOccurrence: EventOccurrence): VEvent = {

		var end: DateTime = eventOccurrence.end.toDateTime
		if (eventOccurrence.start.toDateTime.isEqual(end)) {
			end = end.plusMinutes(1)
		}
		val event: VEvent = new VEvent(toDateTime(eventOccurrence.start.toDateTime), toDateTime(end.toDateTime), eventOccurrence.title.maybeText.getOrElse(eventOccurrence.name).safeSubstring(0, 255))
		event.getStartDate.getParameters.add(Value.DATE_TIME)
		event.getStartDate.getParameters.add(new parameter.TzId("Europe/London"))
		event.getEndDate.getParameters.add(Value.DATE_TIME)
		event.getEndDate.getParameters.add(new parameter.TzId("Europe/London"))

		if (eventOccurrence.description.hasText) {
			event.getProperties.add(new Description(eventOccurrence.description))
		}
		if (eventOccurrence.location.nonEmpty) {
			event.getProperties.add(new Location(eventOccurrence.location.getOrElse("")))
		}

		val uid = DigestUtils.md5Hex(Seq(eventOccurrence.name, eventOccurrence.start.toString, eventOccurrence.end.toString,
			eventOccurrence.location.getOrElse(""), eventOccurrence.context.getOrElse("")).mkString)

		event.getProperties.add(new Uid(uid))
		event.getProperties.add(Method.PUBLISH)
		event.getProperties.add(Transp.OPAQUE)

		eventOccurrence.staffUniversityIds.headOption.flatMap {
			universityId =>
				profileService.getMemberByUniversityId(universityId, disableFilter = true).map {
					staffMember =>
						val organiser: Organizer = new Organizer(s"MAILTO:${staffMember.email}")
						organiser.getParameters.add(new Cn(staffMember.fullName.getOrElse("Unknown")))
						event.getProperties.add(organiser)
				}
		}.getOrElse {
			val organiser: Organizer = new Organizer(s"MAILTO:no-reply@tabula.warwick.ac.uk")
			event.getProperties.add(organiser)
		}

		event
	}

	private def toDateTime(dt: DateTime): net.fortuna.ical4j.model.DateTime = {
		new net.fortuna.ical4j.model.DateTime(dt.getMillis)
	}

}

@Service("termBasedEventOccurrenceService")
class TermBasedEventOccurrenceServiceImpl
	extends TermBasedEventOccurrenceService
	with AutowiringTermServiceComponent
	with AutowiringProfileServiceComponent
	with TermAwareWeekToDateConverterComponent