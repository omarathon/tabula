package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.DateFormats.DateTimePickerFormatter
import uk.ac.warwick.tabula.JavaImports.{JList, _}
import uk.ac.warwick.tabula.commands.{Notifies, SchedulesNotifications, UploadedFile}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{AddsIcalAttachmentToScheduledMeetingNotification, ScheduledMeetingRecordBehalfNotification, ScheduledMeetingRecordInviteeNotification, ScheduledMeetingRecordNotification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

trait AbstractScheduledMeetingCommandInternal extends BindListener {
  self: AbstractScheduledMeetingRecordCommandState
    with MeetingRecordServiceComponent
    with FileAttachmentServiceComponent =>

  def applyCommon(scheduledMeetingRecord: ScheduledMeetingRecord): Unit = {
    scheduledMeetingRecord.title = title
    scheduledMeetingRecord.description = description
    scheduledMeetingRecord.format = format
    scheduledMeetingRecord.lastUpdatedDate = DateTime.now
    if ((!meetingDateStr.isEmptyOrWhitespace) && (!meetingTimeStr.isEmptyOrWhitespace) && (!meetingEndTimeStr.isEmptyOrWhitespace)) {
      scheduledMeetingRecord.meetingDate = DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr)
      scheduledMeetingRecord.meetingEndDate = DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingEndTimeStr)
    }
    scheduledMeetingRecord.meetingLocation = if (meetingLocation.hasText) {
      if (meetingLocationId.hasText) {
        MapLocation(meetingLocation.toString, meetingLocationId)
      } else {
        NamedLocation(meetingLocation.toString)
      }
    } else {
      null
    }
  }

  def persistAttachments(meeting: ScheduledMeetingRecord) {
    // delete attachments that have been removed
    if (meeting.attachments != null) {
      val filesToKeep = Option(attachedFiles).map(_.asScala.toList).getOrElse(List())
      val filesToRemove = meeting.attachments.asScala -- filesToKeep
      meeting.attachments = JArrayList[FileAttachment](filesToKeep)
      fileAttachmentService.deleteAttachments(filesToRemove)
    }

    file.attached.asScala.foreach { attachment =>
      attachment.meetingRecord = meeting
      meeting.attachments.add(attachment)
      attachment.temporary = false
    }
  }

  def onBind(result: BindingResult) {
    file.onBind(result)
  }
}


trait AbstractScheduledMeetingRecordCommandState {
  var title: String = _
  var description: String = _

  var meetingDateStr: String = _
  var meetingTimeStr: String = _
  var meetingEndTimeStr: String = _

  var format: MeetingFormat = _

  var meetingLocation: String = _
  var meetingLocationId: String = _

  var file: UploadedFile = new UploadedFile
  var attachedFiles: JList[FileAttachment] = _

  var attachmentTypes: Seq[String] = Seq[String]()

  var relationships: JList[StudentRelationship] = JArrayList()
}

trait AbstractScheduledMeetingRecordNotifies[T, B] extends Notifies[T, B] {
  def emit(meeting: ScheduledMeetingRecord, user: User, verb: String): Seq[ScheduledMeetingRecordNotification with AddsIcalAttachmentToScheduledMeetingNotification with MyWarwickActivity] = {
    val inviteeNotification = Notification.init(new ScheduledMeetingRecordInviteeNotification(verb), user, meeting)
    if (!meeting.universityIdInRelationship(user.getWarwickId)) {
      val behalfNotification = Notification.init(new ScheduledMeetingRecordBehalfNotification(verb), user, meeting)
      Seq(inviteeNotification, behalfNotification)
    } else {
      Seq(inviteeNotification)
    }
  }
}

trait AbstractScheduledMeetingRecordNotificationProcess {
  def scheduledNotifications(meetingRecord: ScheduledMeetingRecord): Seq[ScheduledNotification[ScheduledMeetingRecord]] = {
    Seq(
      new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderStudent", meetingRecord, meetingRecord.meetingDate.withTimeAtStartOfDay),
      new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderAgent", meetingRecord, meetingRecord.meetingDate.withTimeAtStartOfDay),
      new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordConfirm", meetingRecord, meetingRecord.meetingDate),
      new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordConfirm", meetingRecord, meetingRecord.meetingDate.plusDays(5))
    )
  }
}

trait AbstractScheduledMeetingRecordScheduledNotifications
  extends SchedulesNotifications[ScheduledMeetingRecord, ScheduledMeetingRecord]
    with AbstractScheduledMeetingRecordNotificationProcess {
  override def transformResult(meetingRecord: ScheduledMeetingRecord): Seq[ScheduledMeetingRecord] = Seq(meetingRecord)
}

trait AbstractScheduledMeetingRecordResultScheduledNotifications extends SchedulesNotifications[ScheduledMeetingRecordResult, ScheduledMeetingRecord]
  with AbstractScheduledMeetingRecordNotificationProcess {
  override def transformResult(result: ScheduledMeetingRecordResult): Seq[ScheduledMeetingRecord] = Seq(result.meetingRecord)
}