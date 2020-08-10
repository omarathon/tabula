package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.{MeetingRecord, Notification, StaffMember, StudentMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget, ScopelessPermission}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaBeansWrapper, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}

class MissedMeetingRecordNotificationTest extends TestBase with Mockito with FreemarkerRendering {

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

  val relationshipType: StudentRelationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

  val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

  @Test def singleStudent(): Unit = withUser("cuscav", "0672089") {
    val meeting = new MeetingRecord(student, Seq(relationship))
    meeting.title = "Project catch-up"
    meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 28, 18, 30, 0, 0)

    val notification = Notification.init(new MissedMeetingRecordStudentNotification, agent.asSsoUser, meeting)
    notification.titleFor(student.asSsoUser) should be ("Missed meeting recorded")

    val content = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """Tutor Name recorded a missed personal tutor meeting, which was scheduled for 28 March 2017 at 18:30:00.
        |""".stripMargin
    )
  }

  @Test def singleTutor(): Unit = withUser("cuscav", "0672089") {
    val meeting = new MeetingRecord(student, Seq(relationship))
    meeting.title = "Project catch-up"
    meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 28, 18, 30, 0, 0)

    val notification = Notification.init(new MissedMeetingRecordAgentNotification, agent.asSsoUser, meeting)
    notification.titleFor(agent.asSsoUser) should be ("Missed meeting recorded")

    val content = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """You recorded a missed personal tutor meeting with Student Name, which was scheduled for 28 March 2017 at 18:30:00.
        |""".stripMargin
    )
  }

  @Test def batchStudent(): Unit = withUser("cuscav", "0672089") {
    val notification1 = {
      val meeting = new MeetingRecord(student, Seq(relationship))
      meeting.title = "Project catch-up"
      meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 28, 18, 30, 0, 0)

      Notification.init(new MissedMeetingRecordStudentNotification, agent.asSsoUser, meeting)
    }

    val notification2 = {
      val meeting = new MeetingRecord(student, Seq(relationship))
      meeting.title = "Apology meeting"
      meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 29, 18, 30, 0, 0)

      Notification.init(new MissedMeetingRecordStudentNotification, agent.asSsoUser, meeting)
    }

    val batch = Seq(notification1, notification2)

    MissedMeetingRecordBatchedNotificationHandler.titleForBatch(batch, student.asSsoUser) should be ("2 missed meetings have been recorded")

    val content = MissedMeetingRecordBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """2 meetings have been recorded as missed:
        |
        |- Tutor Name recorded a missed personal tutor meeting, which was scheduled for 28 March 2017 at 18:30:00.
        |- Tutor Name recorded a missed personal tutor meeting, which was scheduled for 29 March 2017 at 18:30:00.
        |""".stripMargin
    )
  }

  @Test def batchTutor(): Unit = withUser("cuscav", "0672089") {
    val notification1 = {
      val meeting = new MeetingRecord(student, Seq(relationship))
      meeting.title = "Project catch-up"
      meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 28, 18, 30, 0, 0)

      Notification.init(new MissedMeetingRecordAgentNotification, agent.asSsoUser, meeting)
    }

    val notification2 = {
      val meeting = new MeetingRecord(student, Seq(relationship))
      meeting.title = "Apology meeting"
      meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 29, 18, 30, 0, 0)

      Notification.init(new MissedMeetingRecordAgentNotification, agent.asSsoUser, meeting)
    }

    val batch = Seq(notification1, notification2)

    MissedMeetingRecordBatchedNotificationHandler.titleForBatch(batch, agent.asSsoUser) should be ("2 missed meetings have been recorded")

    val content = MissedMeetingRecordBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """2 meetings have been recorded as missed:
        |
        |- You recorded a missed personal tutor meeting with Student Name, which was scheduled for 28 March 2017 at 18:30:00.
        |- You recorded a missed personal tutor meeting with Student Name, which was scheduled for 29 March 2017 at 18:30:00.
        |""".stripMargin
    )
  }

}
