package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.DateFormats.{DatePickerFormatter, TimePickerFormatter}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.MeetingFormat._
import uk.ac.warwick.tabula.data.model.{ExternalStudentRelationship, _}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordService, AttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{FileAttachmentService, FileAttachmentServiceComponent, MeetingRecordService, MeetingRecordServiceComponent}

// scalastyle:off magic.number
class CreateMissedMeetingRecordCommandTest extends TestBase with Mockito {

	val aprilFool: DateTime = dateTime(2013, DateTimeConstants.APRIL)
	val marchHare: DateTime = dateTime(2013, DateTimeConstants.MARCH)
	val thisCreator: StaffMember = Fixtures.staff("9876543")
	val student: StudentMember = Fixtures.student(universityId="1170836", userId="studentmember")
	val thisRelationship = ExternalStudentRelationship(
		"Professor A Tutor",
		StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee"),
		student,
		DateTime.now
	)
	
	@Test
	def testCreateMissedMeeting(): Unit = withUser("cuscav") { withFakeTime(aprilFool) {
		val cmd = new CreateMissedMeetingRecordCommandInternal(thisCreator, Seq(thisRelationship))
			with MissedMeetingRecordCommandRequest
			with CreateMeetingRecordCommandState
			with MeetingRecordServiceComponent
			with FeaturesComponent
			with AttendanceMonitoringMeetingRecordServiceComponent
			with FileAttachmentServiceComponent {
			override val meetingRecordService: MeetingRecordService = smartMock[MeetingRecordService]
			override val features: Features = Features.empty
			override val attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
			override val fileAttachmentService: FileAttachmentService = smartMock[FileAttachmentService]

			relationships = JArrayList(thisRelationship)
		}

		cmd.title = "A good title"
		cmd.format = Email
		cmd.meetingDateTime = marchHare
		cmd.meetingDateStr = cmd.meetingDateTime.toString(DatePickerFormatter)
		cmd.meetingTimeStr = cmd.meetingDateTime.toString(TimePickerFormatter)
		cmd.meetingEndTimeStr = cmd.meetingDateTime.plusHours(1).toString(TimePickerFormatter)
		cmd.missedReason = "Junk mail"

		cmd.description = "Lovely words"

		val meeting = cmd.applyInternal()

		meeting should be ('missed)
		meeting.missedReason should be ("Junk mail")
	}}
}
