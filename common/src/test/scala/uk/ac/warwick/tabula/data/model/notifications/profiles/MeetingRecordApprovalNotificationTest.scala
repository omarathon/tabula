package uk.ac.warwick.tabula.data.model.notifications.profiles

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.NewMeetingRecordApprovalNotification
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget, ScopelessPermission}
import uk.ac.warwick.tabula.services.{ProfileService, SecurityService}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaBeansWrapper, ScalaFreemarkerConfiguration, UrlMethodModel}
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}

class MeetingRecordApprovalNotificationTest extends TestBase with Mockito with FreemarkerRendering {

	val securityService: SecurityService = mock[SecurityService]
	securityService.can(any[CurrentUser], any[ScopelessPermission]) returns true
	securityService.can(any[CurrentUser], any[Permission], any[PermissionsTarget]) returns true

	val profileService: ProfileService = mock[ProfileService]

	val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
	freeMarkerConfig.getObjectWrapper.asInstanceOf[ScalaBeansWrapper].securityService = securityService

	val urlMethodModel = new UrlMethodModel
	//urlMethodModel.context = "/profiles"
	urlMethodModel.toplevelUrl = "https://tabula.warwick.ac.uk"

	freeMarkerConfig.setSharedVariable("url", urlMethodModel)

	@Test def itWorks(): Unit = {
		val department = Fixtures.department("es", "Engineering")

		val agent = Fixtures.staff("1234567", "estaff", department)
		agent.firstName = "Karen"
		agent.lastName = "Bradbury"

		val student = Fixtures.student("1218503", "esustu", department)
		student.mostSignificantCourse = Fixtures.studentCourseDetails(student, department)
		student.mostSignificantCourse.sprCode = "1218503/1"

		profileService.getStudentBySprCode("1218503/1") returns Some(student)

		withUser(agent.userId, agent.universityId) {
			val meeting = new MeetingRecord
			meeting.id = "fd269caf-c739-4a44-8f3e-27e79110c73d"
			meeting.creator = agent
			meeting.title = "End of term progress meeting"
			meeting.meetingDate = new DateTime(2013, DateTimeConstants.DECEMBER, 5, 12, 0, 0, 0)

			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
			meeting.relationships = Seq(StudentRelationship(agent, relationshipType, student, DateTime.now))

			val approval = new MeetingRecordApproval
			approval.meetingRecord = meeting
			approval.approver = student
			approval.state = MeetingApprovalState.Pending
			meeting.approvals.add(approval)

			val notification = new NewMeetingRecordApprovalNotification
			notification.agent = agent.asSsoUser
			notification.addItems(Seq(meeting))

			val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			notificationContent should be(
				"""Karen Bradbury has created a record of your personal tutor meeting:

End of term progress meeting at 5 December 2013

This meeting record is pending approval by 1218503 Student.
""")
		}
	}

	@Test def itWorksWithMultipleParticipants(): Unit = {
		val department = Fixtures.department("es", "Engineering")

		val tutor = Fixtures.staff("1234567", "estutor", department)
		tutor.firstName = "Tutor"
		tutor.lastName = "Name"
		val tutorRelationship = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val supervisor = Fixtures.staff("1234568", "esupervisor", department)
		supervisor.firstName = "Supervisor"
		supervisor.lastName = "Name"
		val supervisorRelationship = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")

		val student = Fixtures.student("1218503", "esustu", department)
		student.firstName = "Student"
		student.lastName = "Name"
		student.mostSignificantCourse = Fixtures.studentCourseDetails(student, department)
		student.mostSignificantCourse.sprCode = "1218503/1"

		profileService.getStudentBySprCode("1218503/1") returns Some(student)

		withUser(tutor.userId, tutor.universityId) {
			val meeting = new MeetingRecord
			meeting.id = "fd269caf-c739-4a44-8f3e-27e79110c73d"
			meeting.creator = tutor
			meeting.title = "End of term progress meeting"
			meeting.meetingDate = new DateTime(2013, DateTimeConstants.DECEMBER, 5, 12, 0, 0, 0)

			meeting.relationships = Seq(
				StudentRelationship(tutor, tutorRelationship, student, DateTime.now),
				StudentRelationship(supervisor, supervisorRelationship, student, DateTime.now)
			)

			val studentApproval = new MeetingRecordApproval
			studentApproval.meetingRecord = meeting
			studentApproval.approver = student
			studentApproval.state = MeetingApprovalState.Pending
			meeting.approvals.add(studentApproval)

			val supervisorApproval = new MeetingRecordApproval
			supervisorApproval.meetingRecord = meeting
			supervisorApproval.approver = supervisor
			supervisorApproval.state = MeetingApprovalState.Pending
			meeting.approvals.add(supervisorApproval)

			val notification = new NewMeetingRecordApprovalNotification
			notification.agent = tutor.asSsoUser
			notification.addItems(Seq(meeting))

			notification.title should be ("Meeting record with Student Name, Supervisor Name and Tutor Name needs review")
			notification.titleFor(student.asSsoUser) should be ("Meeting record with Supervisor Name and Tutor Name needs review")
			notification.titleFor(supervisor.asSsoUser) should be ("Meeting record with Student Name and Tutor Name needs review")

			def notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
			notificationContent should be(
				"""Tutor Name has created a record of your meeting with Student Name, Supervisor Name and Tutor Name:

End of term progress meeting at 5 December 2013

This meeting record is pending approval by Student Name and Supervisor Name.
""")

			supervisorApproval.state = MeetingApprovalState.Approved
			notificationContent should endWith ("This meeting record is pending approval by Student Name.\n")

			studentApproval.state = MeetingApprovalState.Approved
			notificationContent should endWith ("This meeting record has been approved.\n")
		}
	}

}