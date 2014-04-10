package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.data.model.{AbstractMeetingRecord, RuntimeMember, StudentMember}
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, RelationshipServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.permissions.Permissions

trait ScheduledMeetingEventSource {
	def occurrencesFor(student: StudentMember, currentUser: CurrentUser): Seq[EventOccurrence]
}
trait ScheduledMeetingEventSourceComponent {
	def scheduledMeetingEventSource: ScheduledMeetingEventSource
}

trait MeetingRecordServiceScheduledMeetingEventSourceComponent extends ScheduledMeetingEventSourceComponent {
	self: RelationshipServiceComponent with MeetingRecordServiceComponent with SecurityServiceComponent =>

	def scheduledMeetingEventSource = new MeetingRecordServiceScheduledMeetingEventSource

	class MeetingRecordServiceScheduledMeetingEventSource extends ScheduledMeetingEventSource {

		def occurrencesFor(student: StudentMember, currentUser: CurrentUser) = {
			def canReadMeeting(meeting: AbstractMeetingRecord) =
				securityService.can(currentUser, Permissions.Profiles.MeetingRecord.Read(meeting.relationship.relationshipType), student)

			val relationships = relationshipService.getAllPastAndPresentRelationships(student).toSet
			val meetings = meetingRecordService.listAll(relationships, currentUser.profile)
			meetings.flatMap { meeting => meeting.toEventOccurrence.map {
				case occurrence if canReadMeeting(meeting) => occurrence
				case occurrence => {
					// No permission to read meeting details, just show as busy
					EventOccurrence.busy(occurrence)
				}
			}}
		}

	}
}