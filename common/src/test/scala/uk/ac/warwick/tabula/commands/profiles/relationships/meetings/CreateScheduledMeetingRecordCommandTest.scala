package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.{DateTime, DateTimeConstants}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.DateFormats._
import uk.ac.warwick.tabula.commands.{ComposableCommand, PopulateOnForm}
import uk.ac.warwick.tabula.data.model.MeetingFormat.Email
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent, FileAttachmentService, FileAttachmentServiceComponent, MeetingRecordService, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class CreateScheduledMeetingRecordCommandTest extends TestBase with Mockito {
  val aprilFool: DateTime = dateTime(2022, DateTimeConstants.APRIL)

  trait Fixture {
    val relationship: StudentRelationship = mock[StudentRelationship]
    var creator: StaffMember = _

    val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]
    mockMeetingRecordService.listScheduled(Set(relationship), Some(creator)) returns Seq()

    val command = new CreateScheduledMeetingRecordCommand(creator, mock[StudentCourseDetails], Seq(relationship))
      with CreateScheduledMeetingRecordCommandValidation
      with AutowiringFileAttachmentServiceComponent
      with AbstractScheduledMeetingCommandInternal
      with MeetingRecordServiceComponent {
      def meetingRecordService: MeetingRecordService = mockMeetingRecordService
    }
    command.relationships.add(relationship)

    val now = new DateTime(2019, DateTimeConstants.NOVEMBER, 1, 13, 40, 19, 0)
  }


  @Test
  def useFirstAgentAsCreatorIfCreatorIsNotInRelationship(): Unit = withUser("cuscav") {
    withFakeTime(aprilFool) {
      val student: StudentMember = Fixtures.student(universityId = "1170836", userId = "studentmember")
      val thisCreator: StaffMember = Fixtures.staff("9876543")
      val thisAgent: StaffMember = Fixtures.staff("9996666")

      val thisRelationship = StudentRelationship(
        thisAgent,
        StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee"),
        student,
        DateTime.now
      )

      val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]
      mockMeetingRecordService.listScheduled(Set(thisRelationship), Some(thisCreator)) returns Seq()

      val command = new CreateScheduledMeetingRecordCommand(thisCreator, mock[StudentCourseDetails], Seq(thisRelationship))
        with CreateScheduledMeetingRecordCommandValidation
        with FileAttachmentServiceComponent
        with AbstractScheduledMeetingCommandInternal
        with MeetingRecordServiceComponent {
        def meetingRecordService: MeetingRecordService = mockMeetingRecordService

        def fileAttachmentService: FileAttachmentService = smartMock[FileAttachmentService]

      }
      command.relationships.add(thisRelationship)

      command.title = "title"
      command.format = MeetingFormat.FaceToFace
      command.meetingDateStr = new DateTime().plusDays(1).toString(DatePickerFormatter)
      command.meetingTimeStr = new DateTime().plusDays(1).toString(TimePickerFormatter)
      command.meetingEndTimeStr = new DateTime().plusDays(1).plusHours(1).toString(TimePickerFormatter)
      command.description = "Lovely words"

      val meeting = command.applyInternal()
      meeting.creator should be(thisAgent)
    }
  }

  @Test
  def validMeeting(): Unit = new Fixture { withFakeTime(now) {
    val errors = new BindException(command, "command")
    command.title = "title"
    command.format = MeetingFormat.FaceToFace
    command.meetingDateStr = new DateTime().plusDays(1).toString(DatePickerFormatter)
    command.meetingTimeStr = new DateTime().plusDays(1).toString(TimePickerFormatter)
    command.meetingEndTimeStr = new DateTime().plusDays(1).plusHours(1).toString(TimePickerFormatter)
    command.validate(errors)
    errors.hasErrors should be(false)
  }}

  @Test
  def noTitle(): Unit = new Fixture { withFakeTime(now) {
    val errors = new BindException(command, "command")
    command.format = MeetingFormat.FaceToFace
    command.meetingDateStr = new DateTime().plusDays(1).toString(DatePickerFormatter)
    command.meetingTimeStr = new DateTime().plusDays(1).toString(TimePickerFormatter)
    command.meetingEndTimeStr = new DateTime().plusDays(1).plusHours(1).toString(TimePickerFormatter)
    command.validate(errors)
    errors.hasErrors should be(true)
    errors.getFieldErrorCount should be(1)
    errors.getFieldErrors("title").size should be(1)
  }}

  @Test
  def noFormat(): Unit = new Fixture { withFakeTime(now) {
    val errors = new BindException(command, "command")
    command.title = "A Meeting"
    val meetingTime: DateTime = new DateTime().plusDays(1)
    command.meetingDateStr = meetingTime.toString(DatePickerFormatter)
    command.meetingTimeStr = meetingTime.toString(TimePickerFormatter)
    command.meetingEndTimeStr = meetingTime.plusHours(1).toString(TimePickerFormatter)
    command.validate(errors)
    errors.hasErrors should be(true)
    errors.getFieldErrorCount should be(1)
    errors.getFieldErrors("format").size should be(1)
  }}

  @Test
  def scheduleInPast(): Unit = new Fixture { withFakeTime(now) {
    val errors = new BindException(command, "command")
    val meetingTime: DateTime = new DateTime().minusWeeks(1)

    command.format = MeetingFormat.FaceToFace
    command.title = "A Title"
    command.meetingDateStr = meetingTime.toString(DatePickerFormatter)
    command.meetingTimeStr = meetingTime.toString(TimePickerFormatter)
    command.meetingEndTimeStr = meetingTime.plusHours(1).toString(TimePickerFormatter)
    command.validate(errors)
    errors.hasErrors should be(true)
    errors.getFieldErrorCount should be(1)
    errors.getFieldErrors("meetingDateStr").size should be(1)
  }}

  @Test
  def scheduleDuplicateDate(): Unit = new Fixture { withFakeTime(now) {
    val meetingTime: DateTime = new DateTime().plusWeeks(1)

    val meetingWithDupeDate: ScheduledMeetingRecord = new ScheduledMeetingRecord
    meetingWithDupeDate.meetingDate = meetingTime

    mockMeetingRecordService.listScheduled(Set(relationship), Some(creator)) returns Seq(meetingWithDupeDate)

    val errors = new BindException(command, "command")
    command.format = MeetingFormat.FaceToFace
    command.title = "A Title"
    command.meetingDateStr = meetingTime.toString(DatePickerFormatter)
    command.meetingTimeStr = meetingTime.toString(TimePickerFormatter)
    command.meetingEndTimeStr = meetingTime.plusHours(1).toString(TimePickerFormatter)

    command.validate(errors)
    errors.hasErrors should be(true)
    errors.getFieldErrorCount should be(1)
    errors.getFieldErrors("meetingDateStr").size should be(1)
  }}

  @Test
  def noInput(): Unit = new Fixture { withFakeTime(now) {
    val errors = new BindException(command, "command")
    command.validate(errors)
    errors.hasErrors should be(true)
  }}
}
