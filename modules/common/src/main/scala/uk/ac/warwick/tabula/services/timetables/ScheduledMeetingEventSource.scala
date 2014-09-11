package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.data.model.{StaffMember, Member, AbstractMeetingRecord, RuntimeMember, StudentMember}
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, RelationshipServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.timetables.{TimetableEvent, EventOccurrence}
import uk.ac.warwick.tabula.permissions.Permissions

trait ScheduledMeetingEventSource {
	def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context): Seq[EventOccurrence]
}
trait ScheduledMeetingEventSourceComponent {
	def scheduledMeetingEventSource: ScheduledMeetingEventSource
}

trait MeetingRecordServiceScheduledMeetingEventSourceComponent extends ScheduledMeetingEventSourceComponent {
	self: RelationshipServiceComponent with MeetingRecordServiceComponent with SecurityServiceComponent =>

	def scheduledMeetingEventSource = new MeetingRecordServiceScheduledMeetingEventSource

	class MeetingRecordServiceScheduledMeetingEventSource extends ScheduledMeetingEventSource {

		def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context) = {
			def canReadMeeting(meeting: AbstractMeetingRecord) =
				securityService.can(currentUser, Permissions.Profiles.MeetingRecord.Read(meeting.relationship.relationshipType), member)

			//Students can also be agents in relationships
			val agentRelationships = relationshipService.listAllStudentRelationshipsWithMember(member).toSet
			val relationships = member match {
				case student: StudentMember => relationshipService.getAllPastAndPresentRelationships(student).toSet ++ agentRelationships
				case _ => agentRelationships
			}

			val meetings = meetingRecordService.listAll(relationships, currentUser.profile)
			meetings.flatMap { meeting => meeting.toEventOccurrence(context).map {
				case occurrence if canReadMeeting(meeting) => occurrence
				case occurrence => {
					// No permission to read meeting details, just show as busy
					EventOccurrence.busy(occurrence)
				}
			}
		}}

	}
}