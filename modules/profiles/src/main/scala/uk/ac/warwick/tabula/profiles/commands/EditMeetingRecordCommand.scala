package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.commands.CompletesNotifications
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{MeetingRecordRejectedNotification, EditedMeetingRecordApprovalNotification}
import uk.ac.warwick.tabula.data.model.{Notification, MeetingRecord, ScheduledNotification}
import collection.JavaConverters._

import scala.language.implicitConversions

class EditMeetingRecordCommand(meetingRecord: MeetingRecord)
	extends ModifyMeetingRecordCommand(meetingRecord.creator, meetingRecord.relationship)
	with FormattedHtml with CompletesNotifications[MeetingRecord]{

	val meeting = meetingRecord

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
		copyToCommand(meetingRecord)
	}

	def copyToCommand(meetingRecord: MeetingRecord) {
		implicit def toOption[T](x:T) : Option[T] = Option(x)

		title = title.getOrElse(meetingRecord.title)
		description = description.getOrElse(meetingRecord.description)
		meetingRecord.isRealTime match {
			case true => meetingDateTime = meetingDateTime.getOrElse(meetingRecord.meetingDate)
			case false => meetingDate = meetingDate.getOrElse(meetingRecord.meetingDate.toLocalDate)
		}
		format = format.getOrElse(meetingRecord.format)
		isRealTime = meetingRecord.isRealTime

		attachedFiles = if(posted){
			// we posted so attachments must have been removed
			attachedFiles.getOrElse(JList())
		} else{
			// we didn't post so attachments must be fetched
			attachedFiles.getOrElse(meetingRecord.attachments)
		}
	}

	override def emit(meeting: MeetingRecord) = Seq(
		Notification.init(new EditedMeetingRecordApprovalNotification, creator.asSsoUser, Seq(meeting), relationship)
	)

	override def transformResult(meetingRecord: MeetingRecord) = Seq(meetingRecord)

	override def scheduledNotifications(result: MeetingRecord) = Seq(
		new ScheduledNotification[MeetingRecord]("editedMeetingRecordApproval", result, DateTime.now.plusWeeks(1))
	)

	def notificationsToComplete(commandResult: MeetingRecord): CompletesNotificationsResult = {
		CompletesNotificationsResult(
			commandResult.approvals.asScala.flatMap(approval =>
				notificationService.findActionRequiredNotificationsByEntityAndType[MeetingRecordRejectedNotification](approval)
			),
			creator.asSsoUser
		)
	}
}