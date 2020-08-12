package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget, ScopelessPermission}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaBeansWrapper, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}

class MeetingRecordApprovedNotificationTest extends TestBase with Mockito with FreemarkerRendering {

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

  @Test def titleStudent(): Unit = withUser("cuscav", "0672089") {
    val meeting = new MeetingRecord(student, Seq(relationship))

    val approval = Fixtures.meetingRecordApproval(state = MeetingApprovalState.Approved)
    approval.meetingRecord = meeting

    val notification = Notification.init(new MeetingRecordApprovedNotification, currentUser.apparentUser, approval)
    notification.titleFor(student.asSsoUser) should be("Personal tutor meeting record with Tutor Name approved")

    notification.recipients should contain only student.asSsoUser
  }

  @Test def titleTutor(): Unit = withUser("cuscav", "0672089") {
    val meeting = new MeetingRecord(agent, Seq(relationship))

    val approval = Fixtures.meetingRecordApproval(state = MeetingApprovalState.Approved)
    approval.meetingRecord = meeting

    val notification = Notification.init(new MeetingRecordApprovedNotification, currentUser.apparentUser, approval)
    notification.titleFor(agent.asSsoUser) should be("Personal tutor meeting record with Student Name approved")

    notification.recipients should contain allOf(agent.asSsoUser, student.asSsoUser)
  }

  @Test def content(): Unit = withUser("cuscav", "0672089") {
    val meeting = new MeetingRecord(student, Seq(relationship))
    meeting.title = "Project catch-up"
    meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 28, 18, 30, 0, 0)

    val approval = Fixtures.meetingRecordApproval(state = MeetingApprovalState.Approved)
    approval.meetingRecord = meeting

    meeting.approvals.add(approval)

    val notification = Notification.init(new MeetingRecordApprovedNotification, student.asSsoUser, approval)

    val content = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """Student Name has approved a record of your personal tutor meeting:
        |
        |Project catch-up on 28 March 2017 at 18:30:00
        |
        |This meeting record has been approved.
        |""".stripMargin
    )
  }

  @Test def batch(): Unit = withUser("cuscav", "0672089") {
    val notification1 = {
      val meeting = new MeetingRecord(student, Seq(relationship))
      meeting.title = "Project catch-up"
      meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 28, 18, 30, 0, 0)

      val approval = Fixtures.meetingRecordApproval(state = MeetingApprovalState.Approved)
      approval.meetingRecord = meeting

      meeting.approvals.add(approval)

      Notification.init(new MeetingRecordApprovedNotification, student.asSsoUser, approval)
    }

    val notification2 = {
      val meeting = new MeetingRecord(student, Seq(relationship))
      meeting.title = "Apology meeting"
      meeting.meetingDate = new DateTime(2017, DateTimeConstants.MARCH, 29, 18, 30, 0, 0)

      val approval = Fixtures.meetingRecordApproval(state = MeetingApprovalState.Approved)
      approval.meetingRecord = meeting

      meeting.approvals.add(approval)

      Notification.init(new MeetingRecordApprovedNotification, student.asSsoUser, approval)
    }

    val batch = Seq(notification1, notification2)

    MeetingRecordBatchedNotificationHandler.titleForBatch(batch, student.asSsoUser) should be ("2 meeting records have been approved")
    MeetingRecordBatchedNotificationHandler.titleForBatch(batch, agent.asSsoUser) should be ("2 meeting records have been approved")

    val content = MeetingRecordBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """2 meetings have been approved:
        |
        |- Project catch-up personal tutor meeting with Student Name on 28 March 2017 at 18:30:00. This meeting record has been approved.
        |- Apology meeting personal tutor meeting with Student Name on 29 March 2017 at 18:30:00. This meeting record has been approved.
        |""".stripMargin
    )
  }

}
