package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget, ScopelessPermission}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaBeansWrapper, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class MeetingRecordApprovalNotificationTest extends TestBase with FreemarkerRendering with Mockito {

  val securityService: SecurityService = mock[SecurityService]
  securityService.can(any[CurrentUser], any[ScopelessPermission]) returns true
  securityService.can(any[CurrentUser], any[Permission], any[PermissionsTarget]) returns true

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
  freeMarkerConfig.getObjectWrapper.asInstanceOf[ScalaBeansWrapper].securityService = securityService

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
    notification.titleFor(student.asSsoUser) should be("Personal tutor meeting record with Tutor Name needs review")
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
    notificationContent should startWith("Tutor Name has created a record of your personal tutor meeting")
  }

  @Test def titleNewMeetingTutor(): Unit = withUser("cuscav", "0672089") {
    val meeting = new MeetingRecord(student, Seq(relationship))

    val notification = Notification.init(new NewMeetingRecordApprovalNotification, currentUser.apparentUser, meeting)
    notification.titleFor(agent.asSsoUser) should be("Personal tutor meeting record with Student Name needs review")
  }

  @Test def titleEditMeetingStudent(): Unit = withUser("cuscav", "0672089") {
    val meeting = new MeetingRecord(agent, Seq(relationship))

    val notification = Notification.init(new EditedMeetingRecordApprovalNotification, currentUser.apparentUser, meeting)
    notification.titleFor(student.asSsoUser) should be("Personal tutor meeting record with Tutor Name needs review")
  }

  @Test def titleEditMeetingTutor(): Unit = withUser("cuscav", "0672089") {
    val meeting = new MeetingRecord(student, Seq(relationship))

    val notification = Notification.init(new EditedMeetingRecordApprovalNotification, currentUser.apparentUser, meeting)
    notification.titleFor(agent.asSsoUser) should be("Personal tutor meeting record with Student Name needs review")
  }

  @Test def batchNew(): Unit = withUser("cuscav", "0672089") {
    val meeting1 = new MeetingRecord(student, Seq(relationship))
    meeting1.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 21, 18, 30, 0, 0)
    meeting1.title = "Project update"

    val meeting2 = new MeetingRecord(student, Seq(relationship))
    meeting2.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 28, 18, 30, 0, 0)
    meeting2.title = "Catch-up"

    val notification1 = Notification.init(new NewMeetingRecordApprovalNotification, currentUser.apparentUser, meeting1)
    val notification2 = Notification.init(new NewMeetingRecordApprovalNotification, currentUser.apparentUser, meeting2)

    val batch = Seq(notification1, notification2)

    MeetingRecordBatchedNotificationHandler.titleForBatch(batch, student.asSsoUser) should be ("2 meeting records have been created")
    MeetingRecordBatchedNotificationHandler.titleForBatch(batch, agent.asSsoUser) should be ("2 meeting records have been created")

    val content = MeetingRecordBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """2 meetings have been created:
        |
        |- Project update personal tutor meeting with Student Name on 21 March 2017 at 18:30:00. This meeting record has been approved.
        |- Catch-up personal tutor meeting with Student Name on 28 March 2017 at 18:30:00. This meeting record has been approved.
        |""".stripMargin
    )
  }

  @Test def batchEdit(): Unit = withUser("cuscav", "0672089") {
    val meeting1 = new MeetingRecord(student, Seq(relationship))
    meeting1.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 21, 18, 30, 0, 0)
    meeting1.title = "Project update"

    val meeting2 = new MeetingRecord(student, Seq(relationship))
    meeting2.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 28, 18, 30, 0, 0)
    meeting2.title = "Catch-up"

    val notification1 = Notification.init(new EditedMeetingRecordApprovalNotification, currentUser.apparentUser, meeting1)
    val notification2 = Notification.init(new EditedMeetingRecordApprovalNotification, currentUser.apparentUser, meeting2)

    val batch = Seq(notification1, notification2)

    MeetingRecordBatchedNotificationHandler.titleForBatch(batch, student.asSsoUser) should be ("2 meeting records have been edited")
    MeetingRecordBatchedNotificationHandler.titleForBatch(batch, agent.asSsoUser) should be ("2 meeting records have been edited")

    val content = MeetingRecordBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """2 meetings have been edited:
        |
        |- Project update personal tutor meeting with Student Name on 21 March 2017 at 18:30:00. This meeting record has been approved.
        |- Catch-up personal tutor meeting with Student Name on 28 March 2017 at 18:30:00. This meeting record has been approved.
        |""".stripMargin
    )
  }

}
