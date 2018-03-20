package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventOccurrenceList
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

import scala.concurrent.Future

trait ScheduledMeetingEventSource {
	def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventOccurrenceList]
}
trait ScheduledMeetingEventSourceComponent {
	def scheduledMeetingEventSource: ScheduledMeetingEventSource
}

trait MeetingRecordServiceScheduledMeetingEventSourceComponent extends ScheduledMeetingEventSourceComponent {
	self: RelationshipServiceComponent with MeetingRecordServiceComponent with SecurityServiceComponent =>

	def scheduledMeetingEventSource = new MeetingRecordServiceScheduledMeetingEventSource

	class MeetingRecordServiceScheduledMeetingEventSource extends ScheduledMeetingEventSource {

		def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context): Future[EventOccurrenceList] = Future {

			def canReadMeetings(relationshipType: StudentRelationshipType) =
				if (currentUser.universityId == member.universityId) true
				else securityService.can(currentUser, Permissions.Profiles.MeetingRecord.Read(relationshipType), member)

			//Students can also be agents in relationships
			val agentRelationships = relationshipService.listCurrentStudentRelationshipsWithMember(member).toSet

			val relationships = member match {
				case student: StudentMember => relationshipService.getAllPastAndPresentRelationships(student).toSet ++ agentRelationships
				case _ => agentRelationships
			}

			val meetings = meetingRecordService.listAll(relationships, currentUser.profile)

			// group the meetings by relationship type and check the permissions for each type only once
			val eventOccurrences = meetings.groupBy(m => HibernateHelpers.initialiseAndUnproxy(m.relationship.relationshipType))
				.mapValues(records => records.flatMap(_.toEventOccurrence(context)))
				.flatMap {
					case (relType, occurrences) if canReadMeetings(relType) => occurrences
					// No permission to read meeting details, just show as busy
					case (_, occurrences) => occurrences.map(EventOccurrence.busy)
				}
			 .toSeq

			EventOccurrenceList.fresh(eventOccurrences)

		}

	}
}

trait AutowiringScheduledMeetingEventSourceComponent extends ScheduledMeetingEventSourceComponent {
	val scheduledMeetingEventSource: (MeetingRecordServiceScheduledMeetingEventSourceComponent with AutowiringRelationshipServiceComponent with AutowiringMeetingRecordServiceComponent with AutowiringSecurityServiceComponent with Object)#MeetingRecordServiceScheduledMeetingEventSource = (new MeetingRecordServiceScheduledMeetingEventSourceComponent
		with AutowiringRelationshipServiceComponent
		with AutowiringMeetingRecordServiceComponent
		with AutowiringSecurityServiceComponent
	).scheduledMeetingEventSource
}