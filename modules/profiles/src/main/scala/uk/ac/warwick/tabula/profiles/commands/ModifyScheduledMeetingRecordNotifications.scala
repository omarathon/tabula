package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.SchedulesNotifications
import uk.ac.warwick.tabula.data.model.{ScheduledMeetingRecord, ScheduledNotification}

trait ModifyScheduledMeetingRecordNotifications extends SchedulesNotifications[ScheduledMeetingRecordResult] {

	override def scheduledNotifications(result: ScheduledMeetingRecordResult) = {
		Seq(
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderStudent", result.meetingRecord, result.meetingRecord.meetingDate.withTimeAtStartOfDay),
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderAgent", result.meetingRecord, result.meetingRecord.meetingDate.withTimeAtStartOfDay)
		)
	}

}
