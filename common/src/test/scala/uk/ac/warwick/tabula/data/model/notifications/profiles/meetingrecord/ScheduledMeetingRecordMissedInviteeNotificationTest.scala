package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ScheduledMeetingRecordMissedInviteeNotificationTest extends TestBase with Mockito {

	@Test def recipient() = withUser("cuscav", "0672089") {
		val agent = Fixtures.staff(currentUser.universityId, currentUser.userId)
		val student = Fixtures.student()

		val relationshipType = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")

		val relationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

		// Scheduled by the agent
		val scheduledMeeting = new ScheduledMeetingRecord(agent, Seq(relationship))
		scheduledMeeting.title = "my meeting"
		scheduledMeeting.description = "discuss things"
		scheduledMeeting.meetingDate = DateTime.now
		scheduledMeeting.format = MeetingFormat.FaceToFace

		val notification = Notification.init(new ScheduledMeetingRecordMissedInviteeNotification(), currentUser.apparentUser, scheduledMeeting)
		notification.recipients.head.getUserId should be (student.userId)

		// if the student creates the meeting instead, recipient should be staff
		scheduledMeeting.creator = student
		notification.recipients.head.getUserId should be (agent.userId)
	}

	trait TitleFixture {
		val agent: StaffMember = Fixtures.staff("1234567", "tutor")
		agent.firstName = "Tutor"
		agent.lastName = "Name"

		val student: StudentMember = Fixtures.student("7654321", "student")
		student.firstName = "Student"
		student.lastName = "Name"

		val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

		val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

		val thirdParty: StaffMember = Fixtures.staff("1122331", "3rdparty")
		thirdParty.firstName = "Third"
		thirdParty.lastName = "Party"
	}

	@Test def titleStudent() { new TitleFixture {
		val meeting = new ScheduledMeetingRecord(agent, Seq(relationship))

		val notification: ScheduledMeetingRecordMissedInviteeNotification = Notification.init(new ScheduledMeetingRecordMissedInviteeNotification, agent.asSsoUser, meeting)
		notification.titleFor(student.asSsoUser) should be ("Scheduled meeting with Tutor Name did not take place")
		notification.recipients should contain only student.asSsoUser
	}}

	@Test def titleTutor() { new TitleFixture {
		val meeting = new ScheduledMeetingRecord(student, Seq(relationship))

		val notification: ScheduledMeetingRecordMissedInviteeNotification = Notification.init(new ScheduledMeetingRecordMissedInviteeNotification, student.asSsoUser, meeting)
		notification.titleFor(agent.asSsoUser) should be ("Scheduled meeting with Student Name did not take place")
		notification.recipients should contain only agent.asSsoUser
	}}

}