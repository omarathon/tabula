package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.data.MeetingRecordDao
import org.hibernate.Session

class ConvertScheduledMeetingRecordCommandTest  extends TestBase with Mockito {

	EventHandling.enabled = false

	trait Fixture {
		val meetingRecordDao = mock[MeetingRecordDao]
		val notificationService = mock[NotificationService]
		val mockMeetingRecordService: MeetingRecordService = mock[MeetingRecordService]

		var creator: StaffMember = Fixtures.staff(universityId = "12345")
		val student = Fixtures.student(universityId = "0123456")
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student)


		val createCommand: CreateMeetingRecordCommand = new CreateMeetingRecordCommand(creator, relationship, false) {
			override val session = 	mock[Session]
		}
		createCommand.meetingRecordDao = meetingRecordDao
		createCommand.features = emptyFeatures
		createCommand.features.attendanceMonitoringMeetingPointType = false
		createCommand.notificationService = notificationService

		val scheduledMeetingRecord: ScheduledMeetingRecord = new ScheduledMeetingRecord()

		val command = new ConvertScheduledMeetingRecordCommand(creator, scheduledMeetingRecord) with ConvertScheduledMeetingRecordCommandSupport with MeetingRecordServiceComponent {
			val meetingRecordService: MeetingRecordService = mockMeetingRecordService
		}
		command.createCommand = createCommand
	}

	@Test
	def apply() { new Fixture {
		val newMeetingRecord = command.applyInternal()

		there was one(createCommand.meetingRecordDao).saveOrUpdate(newMeetingRecord)
		there was one(mockMeetingRecordService).purge(scheduledMeetingRecord)

	}}

	trait ConvertScheduledMeetingRecordCommandSupport extends FileAttachmentServiceComponent {
		def fileAttachmentService = mock[FileAttachmentService]
	}

}
