package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class ScheduledMeetingRecordConfirmNotificationTest extends TestBase {

	val agent: StaffMember = Fixtures.staff("1234567")
	agent.userId = "agent"
	agent.firstName = "Tutor"
	agent.lastName = "Name"

	val student: StudentMember = Fixtures.student("7654321")
	student.userId = "student"
	student.firstName = "Student"
	student.lastName = "Name"

	val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

	val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

	@Test def titleScheduledByStudent() = withUser("cuscav", "0672089") {
		val meeting = new ScheduledMeetingRecord(student, Seq(relationship))

		val notification = Notification.init(new ScheduledMeetingRecordConfirmNotification, currentUser.apparentUser, meeting)
		notification.titleFor(student.asSsoUser) should be ("Meeting record with Tutor Name needs confirmation")
	}

	@Test def titleScheduledByTutor() = withUser("cuscav", "0672089") {
		val meeting = new ScheduledMeetingRecord(agent, Seq(relationship))

		val notification = Notification.init(new ScheduledMeetingRecordConfirmNotification, currentUser.apparentUser, meeting)
		notification.titleFor(agent.asSsoUser) should be ("Meeting record with Student Name needs confirmation")
	}

}
