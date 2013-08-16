package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.services.{UserLookupComponent, SmallGroupServiceComponent}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupFormat, SmallGroupEvent}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat._

trait SmallGroupEventTimetableEventSourceComponent{
	val studentGroupEventSource:StudentTimetableEventSource
}
trait SmallGroupEventTimetableEventSourceComponentImpl extends SmallGroupEventTimetableEventSourceComponent {
	this: SmallGroupServiceComponent with UserLookupComponent =>

	val studentGroupEventSource:StudentTimetableEventSource = new SmallGroupEventTimetableEventSourceImpl

	class SmallGroupEventTimetableEventSourceImpl extends StudentTimetableEventSource{

		def eventsFor(student: StudentMember): Seq[TimetableEvent] = {
			val user = userLookup.getUserByUserId(student.userId)
			val studentsGroups = smallGroupService.findSmallGroupsByStudent(user)
			val allEvents = studentsGroups.flatMap(group => group.events.asScala)
			allEvents map smallGroupEventToTimetableEvent
		}

		def smallGroupEventToTimetableEvent(sge: SmallGroupEvent): TimetableEvent = {
			TimetableEvent(sge)
		}
	}

}
