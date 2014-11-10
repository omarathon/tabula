package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, UserLookupComponent, SmallGroupServiceComponent}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.timetables.TimetableEvent

trait SmallGroupEventTimetableEventSourceComponent{
	val studentGroupEventSource: StudentTimetableEventSource
	val staffGroupEventSource: StaffTimetableEventSource
}
trait SmallGroupEventTimetableEventSourceComponentImpl extends SmallGroupEventTimetableEventSourceComponent {
	self: SmallGroupServiceComponent with UserLookupComponent with SecurityServiceComponent =>

	val studentGroupEventSource: StudentTimetableEventSource = new SmallGroupEventTimetableEventSourceImpl
	val staffGroupEventSource: StaffTimetableEventSource = new SmallGroupEventTimetableEventSourceImpl

	class SmallGroupEventTimetableEventSourceImpl extends StudentTimetableEventSource with StaffTimetableEventSource {

		def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Seq[TimetableEvent] =
			eventsFor(userLookup.getUserByUserId(student.userId), currentUser)

		def eventsFor(staff: StaffMember, currentUser: CurrentUser, context: TimetableEvent.Context): Seq[TimetableEvent] =
			eventsFor(userLookup.getUserByUserId(staff.userId), currentUser)

		private def eventsFor(user: User, currentUser: CurrentUser) = {
			/* Include SGT teaching responsibilities for students (mainly PGR) and students for staff (e.g. Chemistry) */
			val allEvents = studentEvents(user, currentUser) ++ tutorEvents(user, currentUser)
			val autoTimetableEvents = allEvents map smallGroupEventToTimetableEvent

			// TAB-2682 Also include events that the student has been manually added to
			val manualTimetableEvents = smallGroupService.findManuallyAddedAttendance(user.getWarwickId)
				.filter { a =>
					val groupSet = a.occurrence.event.group.groupSet

					!groupSet.deleted && groupSet.visibleToStudents
				}
				.map { a => TimetableEvent(a.occurrence) }

			autoTimetableEvents ++ manualTimetableEvents
		}

		private def studentEvents(user: User, currentUser: CurrentUser) = smallGroupService.findSmallGroupsByStudent(user).filter {
			group =>
				!group.groupSet.deleted &&
				group.events.nonEmpty &&
				(
					// The set is visible to students; OR
					group.groupSet.visibleToStudents ||

					// I have permission to view the membership of the set anyway
					securityService.can(currentUser, Permissions.SmallGroups.ReadMembership, group)
				)
		}.flatMap(group => group.events).filterNot(_.isUnscheduled)

		private def tutorEvents(user: User, currentUser: CurrentUser) = smallGroupService.findSmallGroupEventsByTutor(user).filter {
			event =>
				!event.group.groupSet.deleted &&
				!event.isUnscheduled &&
				(
					// The set is visible to tutors; OR
					event.group.groupSet.releasedToTutors ||

					// I have permission to view the membership of the set anyway
					securityService.can(currentUser, Permissions.SmallGroups.ReadMembership, event)
				)
		}

		private def smallGroupEventToTimetableEvent(sge: SmallGroupEvent): TimetableEvent = {
			TimetableEvent(sge)
		}
	}

}
