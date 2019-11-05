package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{AddsIcalAttachmentToScheduledMeetingNotification, ScheduledMeetingRecordNotification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent, FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object BulkScheduledMeetingRecordCommand {
  def apply(studentRelationships: Seq[StudentRelationship], creator: Member) =
    new BulkScheduledMeetingRecordCommandInternal(studentRelationships, creator)
      with ComposableCommand[Seq[ScheduledMeetingRecord]]
      with ScheduledMeetingRecordValidation
      with BulkScheduledMeetingRecordDescription
      with BulkScheduledMeetingRecordPermissions
      with BulkScheduledMeetingRecordCommandState
      with BulkScheduledMeetingRecordCommandNotifications
      with AutowiringMeetingRecordServiceComponent
      with AutowiringFileAttachmentServiceComponent
      with BulkScheduledMeetingRecordCommandValidation
      with AbstractScheduledMeetingCommandInternal {
      relationships = allRelationships.asJava
    }
}

trait BulkScheduledMeetingRecordCommandValidation extends SelfValidating with ScheduledMeetingRecordValidation {
  self: BulkScheduledMeetingRecordCommandState
    with MeetingRecordServiceComponent =>

  override def validate(errors: Errors) {
    sharedValidation(errors, title, meetingDateStr, meetingTimeStr, meetingEndTimeStr, meetingLocation)
  }
}

class BulkScheduledMeetingRecordCommandInternal(
  val allRelationships: Seq[StudentRelationship],
  val creator: Member,
) extends CommandInternal[Seq[ScheduledMeetingRecord]] with BulkScheduledMeetingRecordCommandState {

  self: MeetingRecordServiceComponent
    with FileAttachmentServiceComponent
    with AbstractScheduledMeetingCommandInternal =>

  override def applyInternal(): Seq[ScheduledMeetingRecord] = {
    allRelationships.map { studentRelationship =>
      new ScheduledMeetingRecord(creator, Seq(studentRelationship))
    }.map { scheduledMeetingRecord =>
      applyCommon(scheduledMeetingRecord)
      scheduledMeetingRecord.creationDate = DateTime.now
      persistAttachments(scheduledMeetingRecord)
      meetingRecordService.saveOrUpdate(scheduledMeetingRecord)
      scheduledMeetingRecord
    }
  }
}


trait BulkScheduledMeetingRecordCommandNotifications extends SchedulesNotifications[Seq[ScheduledMeetingRecord], ScheduledMeetingRecord]
  with AbstractScheduledMeetingRecordNotifies[Seq[ScheduledMeetingRecord], ScheduledMeetingRecord]
  with AbstractScheduledMeetingRecordNotificationProcess {
  self: BulkScheduledMeetingRecordCommandState =>

  override def emit(scheduledMeetingRecords: Seq[ScheduledMeetingRecord]): Seq[ScheduledMeetingRecordNotification with AddsIcalAttachmentToScheduledMeetingNotification] = {
    scheduledMeetingRecords.flatMap { meeting =>
      super.emit(
        meeting = meeting,
        user = creator.asSsoUser,
        verb = "scheduled",
      )
    }
  }

  override def transformResult(meetingRecords: Seq[ScheduledMeetingRecord]): Seq[ScheduledMeetingRecord] = meetingRecords
}


trait BulkScheduledMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: BulkScheduledMeetingRecordCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    allRelationships.foreach { studentRelationship =>
      p.PermissionCheck(Permissions.Profiles.ScheduledMeetingRecord.Manage(studentRelationship.relationshipType), mandatory(studentRelationship.studentMember))
    }
  }
}


trait BulkScheduledMeetingRecordDescription extends Describable[Seq[ScheduledMeetingRecord]] {

  self: BulkScheduledMeetingRecordCommandState =>

  override def describe(d: Description): Unit =
    d.studentRelationships(allRelationships)
     .member(creator)

  override def describeResult(d: Description, scheduledMeetings: Seq[ScheduledMeetingRecord]): Unit =
    d.properties(
      "meetings" -> scheduledMeetings.map(_.id),
      "meetingTitle" -> scheduledMeetings.head.title
    ).fileAttachments(scheduledMeetings.flatMap(_.attachments.asScala))
}

trait BulkScheduledMeetingRecordCommandState extends AbstractScheduledMeetingRecordCommandState {
  def creator: Member

  def allRelationships: Seq[StudentRelationship]
}
