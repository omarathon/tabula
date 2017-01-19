package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, SmallGroupServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.userlookup.User

import scala.concurrent.Future
import scala.util.Try

trait SmallGroupEventTimetableEventSourceComponent{
	val studentGroupEventSource: StudentTimetableEventSource
	val staffGroupEventSource: StaffTimetableEventSource
}

trait SmallGroupEventTimetableEventSourceComponentImpl extends SmallGroupEventTimetableEventSourceComponent {
	self: SmallGroupServiceComponent with UserLookupComponent with SecurityServiceComponent =>

	val studentGroupEventSource: StudentTimetableEventSource = new StudentSmallGroupEventTimetableEventSource
	val staffGroupEventSource: StaffTimetableEventSource = new StaffSmallGroupEventTimetableEventSource

	class StudentSmallGroupEventTimetableEventSource extends StudentTimetableEventSource with SmallGroupEventTimetableEventSource {

		def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventList] =
			Future.fromTry(Try(eventsFor(userLookup.getUserByUserId(student.userId), currentUser)))

	}

	class StaffSmallGroupEventTimetableEventSource extends StaffTimetableEventSource with SmallGroupEventTimetableEventSource {

		def eventsFor(staff: StaffMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventList] =
			Future.fromTry(Try(eventsFor(userLookup.getUserByUserId(staff.userId), currentUser)))

	}

	trait SmallGroupEventTimetableEventSource {

		protected def eventsFor(user: User, currentUser: CurrentUser): EventList = {
			/* Include SGT teaching responsibilities for students (mainly PGR) and students for staff (e.g. Chemistry) */
			val allEvents = studentEvents(user, currentUser) ++ tutorEvents(user, currentUser)

			// TAB-4643 - Don't show event if it has been replaced
			val attendanceMap = smallGroupService.findStudentAttendanceInEvents(user.getWarwickId, allEvents).groupBy(_.occurrence.event)

			val autoTimetableEvents: Seq[TimetableEvent] = allEvents.map { event =>
				attendanceMap.get(event) match {
					case Some(attendanceList) =>
						val weeksToRemove = attendanceList.filter(a => a.state == AttendanceState.MissedAuthorised && !a.replacedBy.isEmpty).map(_.occurrence.week)
						TimetableEvent(event, withoutWeeks = weeksToRemove)
					case None =>
						TimetableEvent(event)
				}
			}

			// TAB-2682 Also include events that the student has been manually added to
			val manualTimetableEvents = smallGroupService.findManuallyAddedAttendance(user.getWarwickId)
				.filter { a =>
					val groupSet = a.occurrence.event.group.groupSet

					!groupSet.deleted && groupSet.visibleToStudents
				}
				.map { a => TimetableEvent(a.occurrence) }

			EventList.fresh(autoTimetableEvents ++ manualTimetableEvents)
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

	}

}
