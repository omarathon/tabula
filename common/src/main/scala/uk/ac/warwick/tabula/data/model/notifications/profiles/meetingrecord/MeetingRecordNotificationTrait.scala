package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{BatchedNotificationHandler, FreemarkerModel, MeetingRecord, Notification}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.userlookup.User

trait MeetingRecordNotificationTrait {
  self: Notification[_, Unit] =>

  def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/meeting_record_notification_template.ftl"

  def meeting: MeetingRecord

  def academicYear: AcademicYear = AcademicYear.forDate(meeting.meetingDate)

  def url: String = Routes.Profile.relationshipType(
    meeting.relationships.head.studentCourseDetails,
    academicYear,
    meeting.relationships.head.relationshipType
  )

  def agentRoles: Seq[String] = meeting.relationships.map(_.relationshipType.agentRole).distinct.sorted

  override def title: String = meeting.relationshipTypes match {
    case Seq(relationshipType) =>
      s"${relationshipType.agentRole.capitalize} meeting with ${meeting.allParticipantNames} $titleSuffix"
    case _ =>
      s"Meeting record with ${meeting.allParticipantNames} $titleSuffix"
  }

  override def titleFor(user: User): String = meeting.relationshipTypes match {
    case Seq(relationshipType) =>
      s"${relationshipType.agentRole.capitalize} meeting record with ${meeting.participantNamesExcept(user)} $titleSuffix"
    case _ =>
      s"Meeting record with ${meeting.participantNamesExcept(user)} $titleSuffix"
  }

  def titleSuffix: String

  def verbed: String
}

object MeetingRecordBatchedNotificationHandler extends BatchedNotificationHandler[Notification[_, Unit] with MeetingRecordNotificationTrait] {
  override def titleForBatchInternal(notifications: Seq[Notification[_, Unit] with MeetingRecordNotificationTrait], user: User): String =
    s"${notifications.size} meeting records have been ${notifications.head.verbed}"

  override def contentForBatchInternal(notifications: Seq[Notification[_, Unit] with MeetingRecordNotificationTrait]): FreemarkerModel =
    FreemarkerModel("/WEB-INF/freemarker/notifications/meetingrecord/meeting_record_notification_template_batch.ftl", Map(
      "verbed" -> notifications.head.verbed,
      "meetings" -> notifications.map(_.content.model),
    ))

  override def urlForBatchInternal(notifications: Seq[Notification[_, Unit] with MeetingRecordNotificationTrait], user: User): String =
    Routes.home

  override def urlTitleForBatchInternal(notifications: Seq[Notification[_, Unit] with MeetingRecordNotificationTrait]): String =
    "view your meetings"
}
