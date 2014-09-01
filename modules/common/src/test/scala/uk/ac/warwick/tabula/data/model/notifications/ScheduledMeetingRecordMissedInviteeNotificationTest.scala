package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.notifications.meetingrecord.ScheduledMeetingRecordMissedInviteeNotification
import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.{MeetingFormat, ScheduledMeetingRecord, StudentRelationshipType, StudentRelationship, MeetingRecord, Notification}
import org.joda.time.DateTime

class ScheduledMeetingRecordMissedInviteeNotificationTest extends TestBase with Mockito {

	@Test def recipient = withUser("cuscav", "0672089") {
		val agent = Fixtures.staff(currentUser.universityId, currentUser.userId)
		val student = Fixtures.student()

		val relationshipType = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")

		val relationship = StudentRelationship(agent, relationshipType, student)

		// Scheduled by the agent
		val scheduledMeeting = new ScheduledMeetingRecord(agent, relationship)
		scheduledMeeting.title = "my meeting"
		scheduledMeeting.description = "discuss things"
		scheduledMeeting.meetingDate = DateTime.now
		scheduledMeeting.format = MeetingFormat.FaceToFace

		val notification = Notification.init(new ScheduledMeetingRecordMissedInviteeNotification(), currentUser.apparentUser, scheduledMeeting, scheduledMeeting.relationship)
		notification.recipient.getUserId should be (student.userId)

		// if the student creates the meeting instead, recipient should be staff
		scheduledMeeting.creator = student
		notification.recipient.getUserId should be (agent.userId)
	}
}