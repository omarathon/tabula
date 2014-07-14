package uk.ac.warwick.tabula.timetables

import org.joda.time.{LocalTime, LocalDateTime}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupFormat, SmallGroupEvent, DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.AcademicYear

case class TimetableEvent(
	name: String,
  title: String,
	description: String,
	eventType: TimetableEventType,
	weekRanges: Seq[WeekRange],
	day: DayOfWeek,
	startTime: LocalTime,
	endTime: LocalTime,
	location: Option[String],
	context: Option[String],
	staffUniversityIds: Seq[String],
	year: AcademicYear
)

object TimetableEvent {

	def apply(sge: SmallGroupEvent): TimetableEvent = {
		TimetableEvent(name = sge.group.name,
			title =  Option(sge.title).getOrElse(""),
			description = sge.group.groupSet.name,
			eventType = smallGroupFormatToTimetableEventType(sge.group.groupSet.format),
			weekRanges = sge.weekRanges,
			day = sge.day,
			startTime = sge.startTime,
			endTime = sge.endTime,
			location = Option(sge.location),
			context = Some(sge.group.groupSet.module.code.toUpperCase),
			staffUniversityIds = sge.tutors.knownType.members,
			year = sge.group.groupSet.academicYear)
	}

	private def smallGroupFormatToTimetableEventType(sgf: SmallGroupFormat): TimetableEventType = sgf match {
		case SmallGroupFormat.Seminar => TimetableEventType.Seminar
		case SmallGroupFormat.Lab => TimetableEventType.Practical
		case SmallGroupFormat.Tutorial => TimetableEventType.Other("Tutorial")
		case SmallGroupFormat.Project => TimetableEventType.Other("Project")
		case SmallGroupFormat.Example => TimetableEventType.Other("Example")
		case SmallGroupFormat.Lecture => TimetableEventType.Lecture
	}

}

sealed abstract class TimetableEventType(val code: String, val displayName: String)

object TimetableEventType {

	case object Lecture extends TimetableEventType("LEC", "Lecture")
	case object Practical extends TimetableEventType("PRA", "Practical")
	case object Seminar extends TimetableEventType("SEM", "Seminar")
	case object Induction extends TimetableEventType("IND", "Induction")
	case object Meeting extends TimetableEventType("MEE", "Meeting")
	case class Other(c: String) extends TimetableEventType(c, c)

	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(Lecture, Practical, Seminar, Induction, Meeting)

	def unapply(code: String): Option[TimetableEventType] = code match {
		case Lecture.code => Some(Lecture)
		case Practical.code => Some(Practical)
		case Seminar.code => Some(Seminar)
		case Induction.code => Some(Induction)
		case Meeting.code => Some(Meeting)
		case _ => None
	}

	def apply(code: String): TimetableEventType = code match {
		case TimetableEventType(t) => t
		case _ => Other(code)
	}
}


case class EventOccurrence(
	name: String,
	title: String,
	description: String,
	eventType: TimetableEventType,
	start: LocalDateTime,
	end: LocalDateTime,
	location: Option[String],
	context: Option[String],
	staffUniversityIds: Seq[String]
)

object EventOccurrence {
	def apply(timetableEvent: TimetableEvent, start: LocalDateTime, end: LocalDateTime): EventOccurrence = {
		EventOccurrence(
			timetableEvent.name,
			timetableEvent.title,
			timetableEvent.description,
			timetableEvent.eventType,
			start,
			end,
			timetableEvent.location,
			timetableEvent.context,
			timetableEvent.staffUniversityIds
		)
	}

	def busy(occurrence: EventOccurrence): EventOccurrence = {
		EventOccurrence(
			"",
			"",
			"",
			TimetableEventType.Other("Busy"),
			occurrence.start,
			occurrence.end,
			None,
			None,
			Nil
		)
	}
}