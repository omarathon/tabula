package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class MeetingRecordApprovedNotificationTest extends TestBase {

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

	@Test def titleStudent() = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(student, Seq(relationship))

		val approval = Fixtures.meetingRecordApproval(state = MeetingApprovalState.Approved)
		approval.meetingRecord = meeting

		val notification = Notification.init(new MeetingRecordApprovedNotification, currentUser.apparentUser, approval)
		notification.titleFor(student.asSsoUser) should be ("Personal tutor meeting record with Tutor Name approved")

		notification.recipients should contain only student.asSsoUser
	}

	@Test def titleTutor() = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(agent, Seq(relationship))

		val approval = Fixtures.meetingRecordApproval(state = MeetingApprovalState.Approved)
		approval.meetingRecord = meeting

		val notification = Notification.init(new MeetingRecordApprovedNotification, currentUser.apparentUser, approval)
		notification.titleFor(agent.asSsoUser) should be ("Personal tutor meeting record with Student Name approved")

		notification.recipients should contain allOf (agent.asSsoUser, student.asSsoUser)
	}

}
