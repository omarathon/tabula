package uk.ac.warwick.tabula.timetables

import org.joda.time.{LocalTime, LocalDateTime}
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Location}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.timetables.TimetableEvent.Parent

case class TimetableEvent(
	uid: String,
	name: String,
  title: String,
	description: String,
	eventType: TimetableEventType,
	weekRanges: Seq[WeekRange],
	day: DayOfWeek,
	startTime: LocalTime,
	endTime: LocalTime,
	location: Option[Location],
	parent: TimetableEvent.Parent,
	comments: Option[String],
	staffUniversityIds: Seq[String],
	studentUniversityIds: Seq[String],
	year: AcademicYear
)

object TimetableEvent {

	sealed trait Context
	object Context {
		case object Student extends Context
		case object Staff extends Context
	}

	sealed trait Parent {
		val shortName: Option[String]
		val fullName: Option[String]
	}
	case class EmptyParent(override val shortName: Option[String], override val fullName: Option[String]) extends Parent
	case class ModuleParent(override val shortName: Option[String], override val fullName: Option[String]) extends Parent
	case class RelationshipParent(override val shortName: Option[String], override val fullName: Option[String]) extends Parent
	object Parent {
		object Empty {
			def apply() = {
				EmptyParent(None, None)
			}
		}
		object Module {
			def apply(module: Option[model.Module]) = {
				ModuleParent(module.map(_.code.toUpperCase), module.map(_.name))
			}
		}
		object Relationship {
			def apply(relationship: StudentRelationshipType) = {
				RelationshipParent(Option(relationship.description), Option(relationship.description))
			}
		}
	}

	def apply(sge: SmallGroupEvent) = eventForSmallGroupEventInWeeks(sge, sge.weekRanges)
	def apply(sgo: SmallGroupEventOccurrence) = eventForSmallGroupEventInWeeks(sgo.event, Seq(WeekRange(sgo.week)))

	private def eventForSmallGroupEventInWeeks(sge: SmallGroupEvent, weekRanges: Seq[WeekRange]): TimetableEvent =
		TimetableEvent(
			uid = sge.id,
			name = sge.group.groupSet.name,
			title = Option(sge.title).getOrElse(""),
			description = s"${sge.group.groupSet.name}: ${sge.group.name}",
			eventType = smallGroupFormatToTimetableEventType(sge.group.groupSet.format),
			weekRanges = weekRanges,
			day = sge.day,
			startTime = sge.startTime,
			endTime = sge.endTime,
			location = Option(sge.location),
			parent = Parent.Module(Option(sge.group.groupSet.module)),
			comments = None,
			staffUniversityIds = sge.tutors.users.map { _.getWarwickId },
			studentUniversityIds = sge.group.students.knownType.members,
			year = sge.group.groupSet.academicYear
		)

	private def smallGroupFormatToTimetableEventType(sgf: SmallGroupFormat): TimetableEventType = sgf match {
		case SmallGroupFormat.Seminar => TimetableEventType.Seminar
		case SmallGroupFormat.Lab => TimetableEventType.Practical
		case SmallGroupFormat.Tutorial => TimetableEventType.Other("Tutorial")
		case SmallGroupFormat.Project => TimetableEventType.Other("Project")
		case SmallGroupFormat.Example => TimetableEventType.Other("Example")
		case SmallGroupFormat.Workshop => TimetableEventType.Other("Workshop")
		case SmallGroupFormat.Lecture => TimetableEventType.Lecture
		case SmallGroupFormat.Exam => TimetableEventType.Other("Exam")
		case SmallGroupFormat.Meeting => TimetableEventType.Meeting
	}

	// Companion object is one of the places searched for an implicit Ordering, so
	// this will be the default when ordering a list of timetable events.
	implicit val defaultOrdering = Ordering.by { event: TimetableEvent => (Option(event.weekRanges).filter(_.nonEmpty).map { _.minBy { _.minWeek }.minWeek }, Option(event.day).map { _.jodaDayOfWeek }, Option(event.startTime).map { _.getMillisOfDay }, Option(event.endTime).map { _.getMillisOfDay }, event.name, event.title, event.uid) }

}

@SerialVersionUID(2903326840601345835l) sealed abstract class TimetableEventType(val code: String, val displayName: String, val core: Boolean = true) extends Serializable

object TimetableEventType {

	case object Lecture extends TimetableEventType("LEC", "Lecture")
	case object Practical extends TimetableEventType("PRA", "Practical")
	case object Seminar extends TimetableEventType("SEM", "Seminar")
	case object Induction extends TimetableEventType("IND", "Induction")
	case object Meeting extends TimetableEventType("MEE", "Meeting")
	case class Other(c: String) extends TimetableEventType(c, c, false)

	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(Lecture, Practical, Seminar, Induction, Meeting)

	def unapply(code: String): Option[TimetableEventType] = code match {
		case Lecture.code | Lecture.displayName => Some(Lecture)
		case Practical.code | Practical.displayName => Some(Practical)
		case Seminar.code | Seminar.displayName => Some(Seminar)
		case Induction.code | Induction.displayName => Some(Induction)
		case Meeting.code | Meeting.displayName => Some(Meeting)
		case _ => None
	}

	def apply(code: String): TimetableEventType = code match {
		case TimetableEventType(t) => t
		case _ => Other(code)
	}
}


case class EventOccurrence(
	uid: String,
	name: String,
	title: String,
	description: String,
	eventType: TimetableEventType,
	start: LocalDateTime,
	end: LocalDateTime,
	location: Option[Location],
	parent: TimetableEvent.Parent,
	comments: Option[String],
	staffUniversityIds: Seq[String]
)

object EventOccurrence {
	def apply(timetableEvent: TimetableEvent, start: LocalDateTime, end: LocalDateTime, uid: String): EventOccurrence = {
		EventOccurrence(
			uid,
			timetableEvent.name,
			timetableEvent.title,
			timetableEvent.description,
			timetableEvent.eventType,
			start,
			end,
			timetableEvent.location,
			timetableEvent.parent,
			timetableEvent.comments,
			timetableEvent.staffUniversityIds
		)
	}

	def busy(occurrence: EventOccurrence): EventOccurrence = {
		EventOccurrence(
			occurrence.uid,
			"",
			"",
			"",
			TimetableEventType.Other("Busy"),
			occurrence.start,
			occurrence.end,
			None,
			Parent.Empty(),
			None,
			Nil
		)
	}
}