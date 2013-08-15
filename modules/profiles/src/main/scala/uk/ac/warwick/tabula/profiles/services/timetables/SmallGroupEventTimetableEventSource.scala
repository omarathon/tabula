package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.services.{UserLookupComponent, SmallGroupServiceComponent}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupFormat, SmallGroupEvent}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat._


trait SmallGroupEventTimetableEventSourceComponent {
	this: SmallGroupServiceComponent with UserLookupComponent =>

	def studentGroupEventSource:StudentTimetableEventSource = new SmallGroupEventTimetableEventSource

	class SmallGroupEventTimetableEventSource extends StudentTimetableEventSource{

		def eventsFor(student: StudentMember): Seq[TimetableEvent] = {
			val user = userLookup.getUserByWarwickUniId(student.universityId)
			val studentsGroups = smallGroupService.findSmallGroupsByStudent(user)
			val allEvents = studentsGroups.flatMap(group => group.events.asScala)
			allEvents map smallGroupEventToTimetableEvent
		}

		def smallGroupEventToTimetableEvent(sge: SmallGroupEvent): TimetableEvent = {
			TimetableEvent(sge)
		}

		def smallGroupFormatToTimetableEventType(sgf: SmallGroupFormat): TimetableEventType = {
			sgf match {
				case Seminar => TimetableEventType.Seminar
				case Lab => TimetableEventType.Practical
				case Tutorial => TimetableEventType.Other("Tutorial")
				case Project => TimetableEventType.Other("Project")
				case Example => TimetableEventType.Other("Example")
			}
		}
	}

}
