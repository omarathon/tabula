package uk.ac.warwick.tabula.profiles.notifications

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.commands._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

trait ScheduledMeetingRecordNotificationFixture {
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
}

class CreateScheduledMeetingRecordNotificationTest extends TestBase with Mockito {
	val notifier = new CreateScheduledMeetingRecordNotification {}

	@Test
	def isAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifications = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def isAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifications = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be ("Meeting created")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be ("Meeting created on your behalf")
	}}
}

class EditScheduledMeetingRecordNotificationTest extends TestBase with Mockito
	with ScheduledMeetingRecordNotificationFixture {

	val notifier = new EditScheduledMeetingRecordNotification {}

	def scheduledMeetingResult(agent: Member, isRescheduled: Boolean = false): ScheduledMeetingRecordResult = {
		val sm = scheduledMeeting(agent)
		ScheduledMeetingRecordResult(sm, isRescheduled)
	}

	@Test
	def isAgent() {
		val notifications = notifier.emit(scheduledMeetingResult(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}

	@Test
	def isAdmin() {
		val notifications = notifier.emit(scheduledMeetingResult(admin))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be ("Meeting updated")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be ("Meeting updated on your behalf")


		val notifications2 = notifier.emit(scheduledMeetingResult(admin, true))
		notifications2.length should be {2}
		notifications2(0).recipients.head should be { student.asSsoUser }
		notifications2(0).title should be ("Meeting rescheduled")
		notifications2(1).recipients.head should be { staff.asSsoUser }
		notifications2(1).title should be ("Meeting rescheduled on your behalf")
	}
}

class DeleteScheduledMeetingRecordNotificationTest extends TestBase with Mockito {
	val notifier = new DeleteScheduledMeetingRecordNotification {}

	@Test
	def isAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifications = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def isAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifications = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be ("Meeting deleted")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be ("Meeting deleted on your behalf")
	}}
}

class RestoreScheduledMeetingRecordNotificationTest extends TestBase with Mockito {
	val notifier = new RestoreScheduledMeetingRecordNotification {}

	@Test
	def isAgent() { new ScheduledMeetingRecordNotificationFixture {
		val notifications = notifier.emit(scheduledMeeting(staff))
		notifications.length should be {1}
		notifications.head.recipients.head should be {student.asSsoUser}
	}}

	@Test
	def isAdmin() { new ScheduledMeetingRecordNotificationFixture {
		val notifications = notifier.emit(scheduledMeeting(admin))
		notifications.length should be {2}
		notifications(0).recipients.head should be { student.asSsoUser }
		notifications(0).title should be ("Meeting rescheduled")
		notifications(1).recipients.head should be { staff.asSsoUser }
		notifications(1).title should be ("Meeting rescheduled on your behalf")
	}}

}

