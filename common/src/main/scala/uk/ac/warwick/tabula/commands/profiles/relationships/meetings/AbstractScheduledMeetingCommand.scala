package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.{DateTime, LocalTime}
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

import scala.jdk.CollectionConverters._
import scala.util.Try

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

    val relatedAgentMembers = Try(scheduledMeetingRecord.agents).toOption.getOrElse(Nil)
    val relatedStudentMembers = Try(scheduledMeetingRecord.relationships.flatMap(_.studentMember)).toOption.getOrElse(Nil)
    if (
      Option(scheduledMeetingRecord.creator).nonEmpty &&
        !relatedAgentMembers.contains(scheduledMeetingRecord.creator) &&
        !relatedStudentMembers.contains(scheduledMeetingRecord.creator)
    ) {
      scheduledMeetingRecord.creator = relatedAgentMembers.headOption.getOrElse(scheduledMeetingRecord.creator)
    }

    persistAttachments(scheduledMeetingRecord)

    // persist the meeting record
    meetingRecordService.saveOrUpdate(scheduledMeetingRecord)
  }

  def persistAttachments(meeting: ScheduledMeetingRecord): Unit = {
    // delete attachments that have been removed
    if (meeting.attachments != null) {
      val filesToKeep = Option(attachedFiles).map(_.asScala.toSeq).getOrElse(Seq())
      val filesToRemove = meeting.attachments.asScala.toSeq diff filesToKeep
      meeting.attachments = JArrayList[FileAttachment](filesToKeep)
      fileAttachmentService.deleteAttachments(filesToRemove)
    }

    val newAttachments = file.attached.asScala.map(_.duplicate())
    newAttachments.foreach(meeting.addAttachment)
  }

  def onBind(result: BindingResult): Unit = {
    result.pushNestedPath("file")
    file.onBind(result)
    result.popNestedPath()
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
  val OnTheDayReminderTime: LocalTime = new LocalTime(8, 0) // 8am

  def scheduledNotifications(meetingRecord: ScheduledMeetingRecord): Seq[ScheduledNotification[ScheduledMeetingRecord]] = {
    val onTheDayReminderDateTime =
      if (meetingRecord.meetingDate.toLocalTime.isBefore(OnTheDayReminderTime))
        meetingRecord.meetingDate.minusHours(1)
      else
        meetingRecord.meetingDate.withTime(OnTheDayReminderTime)

    Seq(
      new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderStudent", meetingRecord, onTheDayReminderDateTime),
      new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderAgent", meetingRecord, onTheDayReminderDateTime),
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
