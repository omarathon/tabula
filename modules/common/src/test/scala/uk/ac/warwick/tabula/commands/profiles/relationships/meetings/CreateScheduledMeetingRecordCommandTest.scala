package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{MeetingRecordService, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.DateFormats.DateTimePickerFormatter
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
		command.meetingDateStr = new DateTime().plusDays(1).toString(DateTimePickerFormatter)
		command.validate(errors)
		errors.hasErrors should be {false}
	}}

	@Test
	def noTitle() { new Fixture {
		val errors = new BindException(command, "command")
		command.format = MeetingFormat.FaceToFace
		command.meetingDateStr = new DateTime().plusDays(1).toString(DateTimePickerFormatter)
		command.validate(errors)
		errors.hasErrors should be {true}
		errors.getFieldErrorCount should be(1)
		errors.getFieldErrors("title").size should be(1)
	}}

	@Test
	def noFormat() { new Fixture {
		val errors = new BindException(command, "command")
		command.title = "A Meeting"
		command.meetingDateStr = new DateTime().plusHours(1).toString(DateTimePickerFormatter)
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
		command.meetingDateStr = new DateTime().minusDays(1).toString(DateTimePickerFormatter)
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
		command.meetingDateStr = meetingTime.toString(DateTimePickerFormatter)
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