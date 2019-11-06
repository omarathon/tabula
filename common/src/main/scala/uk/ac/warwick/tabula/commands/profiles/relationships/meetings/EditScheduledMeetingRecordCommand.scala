package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.DateFormats._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{AddsIcalAttachmentToScheduledMeetingNotification, ScheduledMeetingRecordNotification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent, FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

case class ScheduledMeetingRecordResult(meetingRecord: ScheduledMeetingRecord, isRescheduled: Boolean)

object EditScheduledMeetingRecordCommand {
  def apply(editor: Member, meetingRecord: ScheduledMeetingRecord) =
    new EditScheduledMeetingRecordCommand(editor, meetingRecord)
      with ComposableCommand[ScheduledMeetingRecordResult]
      with EditScheduledMeetingRecordPermissions
      with EditScheduledMeetingRecordState
      with EditScheduledMeetingRecordDescription
      with AutowiringMeetingRecordServiceComponent
      with EditScheduledMeetingRecordCommandValidation
      with EditScheduledMeetingRecordNotification
      with AutowiringFileAttachmentServiceComponent
      with EditScheduledMeetingRecordNotifications
      with PopulateScheduledMeetingRecordCommand
      with AbstractScheduledMeetingCommandInternal
}

class EditScheduledMeetingRecordCommand(val editor: Member, val meetingRecord: ScheduledMeetingRecord)
  extends CommandInternal[ScheduledMeetingRecordResult] with EditScheduledMeetingRecordState {
  self: MeetingRecordServiceComponent
    with FileAttachmentServiceComponent
    with AbstractScheduledMeetingCommandInternal =>

  def applyInternal(): ScheduledMeetingRecordResult = {
    val meetingDate = DateTimePickerFormatter.parseDateTime(meetingRecord.meetingDate.toString(DateTimePickerFormatter))
    val newMeetingDate = DateTimePickerFormatter.parseDateTime(meetingDateStr + " " + meetingTimeStr)
    val isRescheduled = !meetingDate.equals(newMeetingDate)

    applyCommon(meetingRecord)
    persistAttachments(meetingRecord)
    meetingRecordService.saveOrUpdate(meetingRecord)
    ScheduledMeetingRecordResult(meetingRecord, isRescheduled)
  }
}

trait PopulateScheduledMeetingRecordCommand extends PopulateOnForm {

  self: EditScheduledMeetingRecordState =>

  override def populate(): Unit = {
    title = meetingRecord.title
    description = meetingRecord.description

    meetingDateStr = meetingRecord.meetingDate.toString(DatePickerFormatter)
    meetingTimeStr = meetingRecord.meetingDate.withHourOfDay(meetingRecord.meetingDate.getHourOfDay).toString(TimePickerFormatter)
    meetingEndTimeStr = meetingRecord.meetingEndDate.withHourOfDay(meetingRecord.meetingEndDate.getHourOfDay).toString(TimePickerFormatter)

    Option(meetingRecord.meetingLocation).foreach {
      case NamedLocation(name) => meetingLocation = name
      case MapLocation(name, lid, _) =>
        meetingLocation = name
        meetingLocationId = lid
      case AliasedMapLocation(_, MapLocation(name, lid, _)) =>
        meetingLocation = name
        meetingLocationId = lid
    }

    format = meetingRecord.format
    attachedFiles = meetingRecord.attachments

    relationships = meetingRecord.relationships.asJava
  }

}

trait EditScheduledMeetingRecordCommandValidation extends SelfValidating with ScheduledMeetingRecordValidation {
  self: EditScheduledMeetingRecordState with MeetingRecordServiceComponent =>

  override def validate(errors: Errors): Unit = {

    sharedValidation(errors, title, meetingDateStr, meetingTimeStr, meetingEndTimeStr, meetingLocation)

    meetingRecordService.listScheduled(meetingRecord.relationships.toSet, Some(editor)).foreach(
      m => if ((!meetingDateStr.isEmptyOrWhitespace) && (!meetingTimeStr.isEmptyOrWhitespace) && (!meetingEndTimeStr.isEmptyOrWhitespace)) {
        if (m.meetingDate.toString(DateTimePickerFormatter).equals(meetingDateStr + " " + meetingTimeStr) && (m.id != meetingRecord.id)) errors.rejectValue("meetingDateStr", "meetingRecord.date.duplicate")
      }
    )
  }
}

trait ModifyScheduledMeetingRecordState extends AbstractScheduledMeetingRecordCommandState

trait EditScheduledMeetingRecordState extends ModifyScheduledMeetingRecordState {
  def editor: Member

  def meetingRecord: ScheduledMeetingRecord
}

trait EditScheduledMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: EditScheduledMeetingRecordState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mandatory(meetingRecord)
    meetingRecord.relationships.map(_.relationshipType).distinct.foreach { relationshipType =>
      p.PermissionCheck(Permissions.Profiles.ScheduledMeetingRecord.Manage(relationshipType), meetingRecord)
    }
  }
}

trait EditScheduledMeetingRecordDescription extends Describable[ScheduledMeetingRecordResult] {
  self: EditScheduledMeetingRecordState =>

  override lazy val eventName = "EditScheduledMeetingRecord"

  override def describe(d: Description): Unit =
    d.meeting(meetingRecord)
     .member(editor)
}

trait EditScheduledMeetingRecordNotification
  extends AbstractScheduledMeetingRecordNotifies[ScheduledMeetingRecordResult, ScheduledMeetingRecord] {
  self: EditScheduledMeetingRecordState =>

  def emit(result: ScheduledMeetingRecordResult): Seq[ScheduledMeetingRecordNotification with AddsIcalAttachmentToScheduledMeetingNotification] = {
    super.emit(
      meeting = result.meetingRecord,
      user = editor.asSsoUser,
      verb = if (result.isRescheduled) "rescheduled" else "updated",
    )
  }
}

trait EditScheduledMeetingRecordNotifications extends AbstractScheduledMeetingRecordResultScheduledNotifications
