package uk.ac.warwick.tabula.data.model.notifications

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.meetingrecord.ScheduledMeetingRecordInviteeNotification
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ScheduledMeetingRecordInviteeNotificationTest extends TestBase with Mockito {

	// user is an admin that is scheduling a meeting
	@Test def recipient() = withUser("cuslaj", "1170836") {
		val agent = Fixtures.staff(currentUser.universityId, currentUser.userId)

		val staff = Fixtures.staff("9517535", "mctutor")
		val student = Fixtures.student()
		val relationshipType = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")
		val relationship = StudentRelationship(staff, relationshipType, student)

		// Scheduled by the agent
		val scheduledMeeting = new ScheduledMeetingRecord(agent, relationship)
		scheduledMeeting.title = "my meeting"
		scheduledMeeting.description = "discuss things"
		scheduledMeeting.meetingDate = DateTime.now
		scheduledMeeting.format = MeetingFormat.FaceToFace

		val notification = Notification.init(new ScheduledMeetingRecordInviteeNotification("created"), currentUser.apparentUser, scheduledMeeting, scheduledMeeting.relationship)
		notification.recipient.getUserId should be (student.userId)
		// TAB-2489 even if the meeting is scheduled by an admin the tutor should show as the agent in the notification
		notification.content.model("actor").asInstanceOf[User].getWarwickId should be (staff.universityId)

		// if the student creates the meeting, recipient should be staff
		scheduledMeeting.creator = student
		notification.agent = student.asSsoUser
		notification.recipient.getUserId should be (staff.userId)
		notification.content.model("actor").asInstanceOf[User].getWarwickId should be (student.universityId)

		// if the staff schedules the meeting, recipient should be student
		scheduledMeeting.creator = staff
		notification.agent = staff.asSsoUser
		notification.recipient.getUserId should be (student.userId)
		notification.content.model("actor").asInstanceOf[User].getWarwickId should be (staff.universityId)
	}

}
