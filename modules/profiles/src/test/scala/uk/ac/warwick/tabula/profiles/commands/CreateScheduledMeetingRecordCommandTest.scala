package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.{MeetingRecordServiceComponent, MeetingRecordService}
import org.joda.time.DateTime

class CreateScheduledMeetingRecordCommandTest  extends TestBase with Mockito {

	trait Fixture {
		val relationship: StudentRelationship[_] = mock[StudentRelationship[_]]

		val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]
		mockMeetingRecordService.listScheduled(Set(relationship), creator) returns Seq()

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
		errors.hasErrors should be (false)
	}}

	@Test
	def noTitle() { new Fixture {
		val errors = new BindException(command, "command")
		command.format = MeetingFormat.FaceToFace
		command.meetingDate = new DateTime().plusDays(1)
		command.validate(errors)
		errors.hasErrors should be (true)
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("title").size should be(1)
	}}

	@Test
	def noFormat() { new Fixture {
		val errors = new BindException(command, "command")
		command.title = "A Meeting"
		command.meetingDate = new DateTime().plusHours(1)
		command.validate(errors)
		errors.hasErrors should be (true)
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
		errors.hasErrors should be (true)
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("meetingDate").size should be(1)
	}}

	@Test
	def scheduleDuplicateDate() { new Fixture {

		val meetingTime: DateTime = new DateTime().plusWeeks(1)

		val meetingWithDupeDate: ScheduledMeetingRecord = new ScheduledMeetingRecord
		meetingWithDupeDate.meetingDate = meetingTime

		mockMeetingRecordService.listScheduled(Set(relationship), creator) returns Seq(meetingWithDupeDate)

		val errors = new BindException(command, "command")
		command.format = MeetingFormat.FaceToFace
		command.title = "A Title"
		command.meetingDate = meetingTime
		command.validate(errors)
		errors.hasErrors should be (true)
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("meetingDate").size should be(1)
	}}

	@Test
	def noInput() { new Fixture {
		val errors = new BindException(command, "command")
		command.validate(errors)
		errors.hasErrors should be (true)
	}}
}
