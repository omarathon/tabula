package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.notifications.meetingrecord.{ScheduledMeetingRecordInviteeNotification, ScheduledMeetingRecordBehalfNotification}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services.{MeetingRecordServiceComponent, MeetingRecordService}
import org.joda.time.DateTime

class CreateScheduledMeetingRecordCommandTest extends TestBase with Mockito {

	trait Fixture {
		val relationship: StudentRelationship = mock[StudentRelationship]

		val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]
		mockMeetingRecordService.listScheduled(Set(relationship), Some(creator)) returns Seq()

		var creator: StaffMember = _
		val command = new CreateScheduledMeetingRecordCommand(creator, relationship, false) with CreateScheduledMeetingRecordCommandValidation with MeetingRecordServiceComponent {
			val meetingRecordService: MeetingRecordService = mockMeetingRecordService
		}
	}

	@Test
	def validMeeting() { new Fixture {
		val errors = new BindException(command, "command")
		command.title = "title"
		command.format = MeetingFormat.FaceToFace
		command.meetingDate = new DateTime().plusDays(1)
		command.validate(errors)
		errors.hasErrors should be {false}
	}}

	@Test
	def noTitle() { new Fixture {
		val errors = new BindException(command, "command")
		command.format = MeetingFormat.FaceToFace
		command.meetingDate = new DateTime().plusDays(1)
		command.validate(errors)
		errors.hasErrors should be {true}
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("title").size should be(1)
	}}

	@Test
	def noFormat() { new Fixture {
		val errors = new BindException(command, "command")
		command.title = "A Meeting"
		command.meetingDate = new DateTime().plusHours(1)
		command.validate(errors)
		errors.hasErrors should be {true}
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("format").size should be(1)
	}}

	@Test
	def scheduleInPast() { new Fixture {
		val errors = new BindException(command, "command")
		command.format = MeetingFormat.FaceToFace
		command.title = "A Title"
		command.meetingDate = new DateTime().minusDays(1)
		command.validate(errors)
		errors.hasErrors should be {true}
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("meetingDate").size should be(1)
	}}

	@Test
	def scheduleDuplicateDate() { new Fixture {

		val meetingTime: DateTime = new DateTime().plusWeeks(1)

		val meetingWithDupeDate: ScheduledMeetingRecord = new ScheduledMeetingRecord
		meetingWithDupeDate.meetingDate = meetingTime

		mockMeetingRecordService.listScheduled(Set(relationship), Some(creator)) returns Seq(meetingWithDupeDate)

		val errors = new BindException(command, "command")
		command.format = MeetingFormat.FaceToFace
		command.title = "A Title"
		command.meetingDate = meetingTime
		command.validate(errors)
		errors.hasErrors should be {true}
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("meetingDate").size should be(1)
	}}

	@Test
	def noInput() { new Fixture {
		val errors = new BindException(command, "command")
		command.validate(errors)
		errors.hasErrors should be {true}
	}}
}

class CreateScheduledMeetingRecordNotificationTest extends TestBase with Mockito {
	trait Fixture {

		val admin = Fixtures.staff("1170836", "cuslaj")
		val staff = Fixtures.staff("9517535", "mctutor")
		val student = Fixtures.student()
		val relationshipType = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")
		val relationship = StudentRelationship(staff, relationshipType, student)

		def scheduledMeeting(agent: Member): ScheduledMeetingRecord = {
			val scheduledMeeting = new ScheduledMeetingRecord(agent, relationship)
			scheduledMeeting.title = "my meeting"
			scheduledMeeting.description = "discuss things"
			scheduledMeeting.meetingDate = DateTime.now
			scheduledMeeting.format = MeetingFormat.FaceToFace
			scheduledMeeting
		}

		val notifier = new CreateScheduledMeetingRecordNotification {}
	}

	@Test
	def isAgent() { new Fixture {
		val notifications = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def isAdmin() { new Fixture {
		val notifications = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be ("Meeting created")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be ("Meeting created on your behalf")
	}}
}