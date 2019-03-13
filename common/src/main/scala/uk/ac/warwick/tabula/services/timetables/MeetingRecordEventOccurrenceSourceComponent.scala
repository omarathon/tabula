package uk.ac.warwick.tabula.services.timetables

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventOccurrenceList
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable

import scala.concurrent.Future

trait MeetingRecordEventOccurrenceSourceComponent extends EventOccurrenceSourceComponent {
	self: RelationshipServiceComponent with MeetingRecordServiceComponent with SecurityServiceComponent =>

	def eventOccurrenceSource = new MeetingRecordEventOccurrenceSource

	class MeetingRecordEventOccurrenceSource extends EventOccurrenceSource {

		def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context, start: LocalDate, end: LocalDate): Future[EventOccurrenceList] = Future {

			def canReadMeetings(relationshipType: StudentRelationshipType) =
				if (currentUser.universityId == member.universityId) true
				else securityService.can(currentUser, Permissions.Profiles.MeetingRecord.Read(relationshipType), member)

			//Students can also be agents in relationships
			val agentRelationships = relationshipService.listCurrentStudentRelationshipsWithMember(member).toSet

			val relationships = member match {
				case student: StudentMember => relationshipService.getAllPastAndPresentRelationships(student).toSet ++ agentRelationships
				case _ => agentRelationships
			}

			// FIXME apply date filtering in the DB so we don't have to filter in code at the end
			val meetings = meetingRecordService.listAll(relationships, currentUser.profile)

			// group the meetings by relationship type and check the permissions for each type only once
			val eventOccurrences = meetings.groupBy(m => HibernateHelpers.initialiseAndUnproxy(m.relationshipTypes))
				.mapValues(records => records.flatMap(_.toEventOccurrence(context)))
				.flatMap {
					case (relType, occurrences) if relType.exists(canReadMeetings) => occurrences
					// No permission to read meeting details, just show as busy
					case (_, occurrences) => occurrences.map(EventOccurrence.busy)
				}
			 .toSeq

			EventOccurrenceList.fresh(eventOccurrences)
				.map(_.filterNot { event =>
					event.end.toLocalDate.isBefore(start) || event.start.toLocalDate.isAfter(end)
				})

		}

	}
}

