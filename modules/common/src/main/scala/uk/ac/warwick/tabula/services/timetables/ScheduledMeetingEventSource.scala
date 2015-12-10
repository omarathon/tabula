package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{AbstractMeetingRecord, Member, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

import scala.concurrent.Future
import scala.util.{Success, Try}

trait ScheduledMeetingEventSource {
	def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context): Future[Seq[EventOccurrence]]
}
trait ScheduledMeetingEventSourceComponent {
	def scheduledMeetingEventSource: ScheduledMeetingEventSource
}

trait MeetingRecordServiceScheduledMeetingEventSourceComponent extends ScheduledMeetingEventSourceComponent {
	self: RelationshipServiceComponent with MeetingRecordServiceComponent with SecurityServiceComponent =>

	def scheduledMeetingEventSource = new MeetingRecordServiceScheduledMeetingEventSource

	class MeetingRecordServiceScheduledMeetingEventSource extends ScheduledMeetingEventSource {

		def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context): Future[Seq[EventOccurrence]] = {
			def canReadMeeting(meeting: AbstractMeetingRecord) =
				securityService.can(currentUser, Permissions.Profiles.MeetingRecord.Read(meeting.relationship.relationshipType), member)

			//Students can also be agents in relationships
			val agentRelationships = relationshipService.listAllStudentRelationshipsWithMember(member).toSet
			val relationships = member match {
				case student: StudentMember => relationshipService.getAllPastAndPresentRelationships(student).toSet ++ agentRelationships
				case _ => agentRelationships
			}

			val meetings = meetingRecordService.listAll(relationships, currentUser.profile)
			Future.successful(meetings.flatMap { meeting => meeting.toEventOccurrence(context).map {
				case occurrence if canReadMeeting(meeting) => occurrence
				case occurrence =>
					// No permission to read meeting details, just show as busy
					EventOccurrence.busy(occurrence)
			}})
		}

	}
}

trait AutowiringScheduledMeetingEventSourceComponent extends ScheduledMeetingEventSourceComponent {
	val scheduledMeetingEventSource = (new MeetingRecordServiceScheduledMeetingEventSourceComponent
		with AutowiringRelationshipServiceComponent
		with AutowiringMeetingRecordServiceComponent
		with AutowiringSecurityServiceComponent
	).scheduledMeetingEventSource
}