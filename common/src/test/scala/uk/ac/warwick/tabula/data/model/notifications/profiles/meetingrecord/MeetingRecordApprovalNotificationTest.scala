package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaBeansWrapper, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class MeetingRecordApprovalNotificationTest extends TestBase with FreemarkerRendering with Mockito {

	val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
	freeMarkerConfig.getObjectWrapper.asInstanceOf[ScalaBeansWrapper].securityService = smartMock[SecurityService]

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

	@Test def titleNewMeetingStudent(): Unit = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(agent, Seq(relationship))

		val notification = Notification.init(new NewMeetingRecordApprovalNotification, currentUser.apparentUser, meeting)
		notification.titleFor(student.asSsoUser) should be ("Personal tutor meeting record with Tutor Name needs review")
	}

	// TAB-5119
	@Test def rendersOutsideThreadNewMeetingStudent(): Unit = {
		val u = new User("cuscav")
		u.setIsLoggedIn(true)
		u.setFoundUser(true)
		u.setWarwickId("0672089")

		val meeting = new MeetingRecord(agent, Seq(relationship))
		meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 21, 18, 30, 0, 0)

		val notification = Notification.init(new NewMeetingRecordApprovalNotification, u, meeting)

		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should startWith ("Tutor Name has created a record of your personal tutor meeting")
	}

	@Test def titleNewMeetingTutor(): Unit = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(student, Seq(relationship))

		val notification = Notification.init(new NewMeetingRecordApprovalNotification, currentUser.apparentUser, meeting)
		notification.titleFor(agent.asSsoUser) should be ("Personal tutor meeting record with Student Name needs review")
	}

	@Test def titleEditMeetingStudent(): Unit = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(agent, Seq(relationship))

		val notification = Notification.init(new EditedMeetingRecordApprovalNotification, currentUser.apparentUser, meeting)
		notification.titleFor(student.asSsoUser) should be ("Personal tutor meeting record with Tutor Name needs review")
	}

	@Test def titleEditMeetingTutor(): Unit = withUser("cuscav", "0672089") {
		val meeting = new MeetingRecord(student, Seq(relationship))

		val notification = Notification.init(new EditedMeetingRecordApprovalNotification, currentUser.apparentUser, meeting)
		notification.titleFor(agent.asSsoUser) should be ("Personal tutor meeting record with Student Name needs review")
	}

}
